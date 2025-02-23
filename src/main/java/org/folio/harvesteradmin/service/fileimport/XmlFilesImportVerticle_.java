package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.*;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class XmlFilesImportVerticle_ extends AbstractVerticle {

    private final String tenant;
    private final UUID jobConfigId;
    private ImportJob importJob;
    public static final Logger logger = LogManager.getLogger("queued-files-processing");
    private final static ConcurrentMap<String, ConcurrentMap<String, String>> fileImportVerticles = new ConcurrentHashMap<>();
    private final RoutingContext routingContext;

    FileQueue fileQueue;


    public XmlFilesImportVerticle_(String tenant, String jobConfigId, Vertx vertx, RoutingContext routingContext) {
        this.tenant = tenant;
        this.jobConfigId = UUID.fromString(jobConfigId);
        this.routingContext = routingContext;
        this.fileQueue = new FileQueue(vertx, tenant, jobConfigId);
    }

    public static void launchVerticle(String tenant, String importConfigurationId, RoutingContext routingContext) {
        fileImportVerticles.putIfAbsent(tenant, new ConcurrentHashMap<>());
        String previousMapping = fileImportVerticles.get(tenant).putIfAbsent(importConfigurationId, "initializing");
        if (previousMapping == null) {
            Vertx vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(10).setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES));
            vertx.deployVerticle(new XmlFilesImportVerticle_(tenant, importConfigurationId, vertx, routingContext), new DeploymentOptions()
                    .setWorkerPoolSize(1).setMaxWorkerExecuteTime(10).setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES)).onComplete(
                    started -> {
                        if (started.succeeded()) {
                            System.out.println("ID-NE: started verticle for " + tenant + " and configuration ID " + importConfigurationId);
                            System.out.println("ID-NE: current thread " + Thread.currentThread().getName());
                            fileImportVerticles.get(tenant).put(importConfigurationId, started.result());
                            System.out.println("ID-NE: deployed verticles: " + vertx.deploymentIDs());
                        } else {
                            System.out.println("ID-NE: Couldn't start file processor threads for tenant " + tenant + " and import configuration ID " + importConfigurationId);
                        }
                    });
        } else {
            System.out.println("ID-NE: Continuing with already existing verticle for tenant " + tenant + " and import configuration ID " + importConfigurationId);
        }
    }

    @Override
    public void start() {
        System.out.println("ID-NE: starting file processor for tenant " + tenant + " and job configuration ID " + jobConfigId);
        vertx.setPeriodic(200, (r) -> {
            File currentFile = getNextFileIfPossible();
            if (currentFile != null) {  // null if queue is empty or a previous file is still processing
                boolean activating = fileQueue.passive.getAndSet(false); // check if job was passive before this file
                // Use existing job or instantiate new.
                getJob(activating)
                        .compose(job -> job.processFile(currentFile))
                        .onComplete(na -> fileQueue.deleteFile(currentFile))
                        .onFailure(f -> System.out.println("Error processing file: " + f.getMessage()));
            }
        });
    }

    public File getNextFileIfPossible () {
        if (importJob != null && importJob.resumeHaltedProcessing()) {
            return fileQueue.currentlyPromotedFile();
        } else {
            return fileQueue.nextFileIfPossible();
        }
    }

    public Future<ImportJob> getJob (boolean activating) {
        if (activating) {
            return ImportJob.instantiateJob(tenant, jobConfigId, fileQueue, vertx, routingContext)
                    .compose(job -> {
                        importJob = job;
                        return Future.succeededFuture(importJob);
                    });
        } else {
            return Future.succeededFuture(importJob);
        }
    }

}