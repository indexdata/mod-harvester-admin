package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.moduledata.ImportConfig;
import org.folio.harvesteradmin.moduledata.ImportJobLog;
import org.folio.harvesteradmin.moduledata.database.ModuleStorageAccess;
import org.folio.harvesteradmin.service.fileimport.transformation.TransformationPipeline;
import org.folio.harvesteradmin.utils.SettableClock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;


public class ImportJob {
    UUID importConfigId;
    ImportJobLog importJobLog;
    Reporting_ reporting;
    FileQueue fileQueue;
    TransformationPipeline transformationPipeline;
    InventoryBatchUpdater_ updater;
    Vertx vertx;

    ModuleStorageAccess configStorage;

    private ImportJob(Vertx vertx, String tenant, UUID importConfigId) {
        this.vertx = vertx;
        this.importConfigId = importConfigId;
        this.configStorage = new ModuleStorageAccess(vertx, tenant);
    }

    public static Future<ImportJob> instantiateJob (String tenant, UUID jobConfigId, FileQueue fileQueue, Vertx vertx, RoutingContext routingContext) {
        ImportJob job = new ImportJob(vertx, tenant, jobConfigId);
        job.fileQueue = fileQueue;                          // Handle to source files in vertx file system
        job.reporting = new Reporting_(job, tenant, vertx); // Logging progress and results
        job.updater = new InventoryBatchUpdater_(job, routingContext); // Batching and persisting records in inventory.
        return job.initiateJobLog(jobConfigId)
                .compose(na -> job.getTransformationPipeline(tenant, jobConfigId, vertx))
                .compose(na -> Future.succeededFuture(job));
    }

    public boolean fileQueueDone(boolean atEndOfCurrentFile) {
        if (atEndOfCurrentFile && !fileQueue.hasNextFile() && !reporting.pendingFileStats()) {
            fileQueue.passive.set(true);
        }
        return fileQueue.passive.get();
    }

    public void setFinishedDateTime() {
        importJobLog.setFinished(SettableClock.getLocalDateTime(), configStorage);
    }

    public UUID jobConfigId () {
        return importConfigId;
    }

    Future<Void> processFile(File xmlFile) {
        Promise<Void> promise = Promise.promise();
        try {
            reporting.nowProcessing(xmlFile.getName());
            String xmlFileContents = Files.readString(xmlFile.toPath(), StandardCharsets.UTF_8);
            vertx.executeBlocking(new XmlRecordsFromFile(xmlFileContents).setTarget(transformationPipeline))
                            .onComplete(processing -> {
                                if (processing.succeeded()) {
                                    promise.complete();
                                } else {
                                    System.out.println("Processing failed with " + processing.cause().getMessage());
                                    promise.complete();
                                }
                            });

        } catch (IOException e) {
            promise.fail("Could not open XML source file for importing " + e.getMessage());
        }
        return promise.future();
    }


    /**
     * If there's a file in the processing slot but no activity in the inventory updater, the current job
     * is assumed to be in a paused state, which could for example be due to a module restart.
     * @return true if there's a file ostensibly processing but no activity detected in inventory updater
     * for `idlingChecksThreshold` consecutive checks
     */
    public boolean resumeHaltedProcessing() {
        return fileQueue.processingSlotTaken() && updater.noPendingBatches(10);
    }

    private Future<UUID> initiateJobLog (UUID importConfigId) {
        return configStorage.getEntityById(importConfigId, new ImportConfig())
                .compose(importConfig -> {
                    importJobLog = new ImportJobLog().fromImportConfig((ImportConfig) importConfig);
                    return configStorage.storeEntity(importJobLog);
                });
    }

    private Future<TransformationPipeline> getTransformationPipeline(String tenant, UUID importConfigId, Vertx vertx) {
        Promise<TransformationPipeline> promise = Promise.promise();
        new ModuleStorageAccess(vertx, tenant).getEntityById(importConfigId,new ImportConfig())
                .map(cfg -> ((ImportConfig) cfg).record.transformationId())
                .compose(transformationId -> TransformationPipeline._create(vertx, tenant, importConfigId.toString(), transformationId))
                .onComplete(pipelineBuild -> {
                    transformationPipeline = pipelineBuild.result();
                    transformationPipeline.setTarget(updater);
                    promise.complete(pipelineBuild.result());
                })
                .onFailure(e -> Future.failedFuture(e.getMessage()));
        return promise.future();
    }

}
