package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage;
import org.folio.harvesteradmin.service.fileimport.transformation.TransformationPipeline;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;

public class XmlFilesImporter extends AbstractVerticle {

    private final String tenant;
    private final String jobConfigId;
    private final FileQueue fileQueue;
    private final Reporting reporting;
    private final InventoryBatchUpdater inventoryUpdater;
    private final AtomicBoolean passive = new AtomicBoolean(true);
    public static final Logger logger = LogManager.getLogger("queued-files-processing");

    public XmlFilesImporter(String tenant, String jobConfigId, Vertx vertx, RoutingContext routingContext) {
        this.tenant = tenant;
        this.jobConfigId = jobConfigId;
        fileQueue = new FileQueue(vertx, tenant, jobConfigId);
        reporting = new Reporting(this);
        inventoryUpdater = new InventoryBatchUpdater(this, routingContext);
    }

    @Override
    public void start() {
        System.out.println("ID-NE: starting file processor for tenant " + tenant + " and job configuration ID " + jobConfigId);
        vertx.setPeriodic(200, (r) -> {
            File currentFile = nextFileIfPossible();
            if (currentFile != null) {  // null if queue is empty or a previous file is still processing
                boolean activating = passive.getAndSet(false); // check if job was passive before this file
                reporting.nowProcessing(currentFile.getName(), activating); // reset stats if new job was just activated
                processFile(currentFile, activating) // refresh style sheets from db if new job
                        .onComplete(na -> fileQueue.deleteFile(currentFile))
                        .onFailure(f -> System.out.println("Error processing file: " + f.getMessage()));
            }
        });
    }

    private File nextFileIfPossible() {
        if (resumeHaltedProcessing()) {
            return fileQueue.currentlyPromotedFile();
        } else if (fileQueue.promoteNextFileIfPossible()) {
            return fileQueue.currentlyPromotedFile();
        }
        return null;
    }

    public boolean fileQueueDone(boolean possibly) {
        if (possibly && !fileQueue.hasNextFile() && !reporting.pendingFileStats()) {
            passive.set(true);
        }
        return passive.get();
    }

    /**
     * If there's a file in the processing slot but no activity in the inventory updater, the current job
     * is assumed to be in a paused state, which could for example be due to a module restart.
     * @return true if there's a file ostensibly processing but no activity detected in inventory updater
     * for `idlingChecksThreshold` consecutive checks
     */
    private boolean resumeHaltedProcessing() {
        return fileQueue.processingSlotTaken() && inventoryUpdater.noPendingBatches(10);
    }

    private Future<Void> processFile(File xmlFile, boolean refreshPipeline) {
        Promise<Void> promise = Promise.promise();
        try {
            String xmlFileContents = Files.readString(xmlFile.toPath(), StandardCharsets.UTF_8);
            getTransformationPipeline(jobConfigId, refreshPipeline)
                    .map(transformationPipeline -> transformationPipeline.setTarget(inventoryUpdater))
                    .compose(pipelineToInventory -> vertx
                            .executeBlocking(new XmlRecordsFromFile(xmlFileContents).setTarget(pipelineToInventory))
                            .onComplete(processing -> {
                                if (processing.succeeded()) {
                                    promise.complete();
                                } else {
                                    System.out.println("Processing failed with " + processing.cause().getMessage());
                                    promise.complete();
                                }
                            }));
        } catch (IOException e) {
            promise.fail("Could not open XML source file for importing " + e.getMessage());
        }
        return promise.future();
    }

    private Future<TransformationPipeline> getTransformationPipeline(String jobConfigId, boolean refresh) {
        Promise<TransformationPipeline> promise = Promise.promise();
        if (TransformationPipeline.hasInstance(tenant, jobConfigId) && !refresh) {
            promise.complete(TransformationPipeline.getInstance(tenant, jobConfigId));
        } else {
            new LegacyHarvesterStorage(vertx, tenant).getConfigRecordById(HARVESTER_HARVESTABLES_PATH, jobConfigId)
                    .compose(resp -> {
                        if (resp.wasOK()) {
                            JsonObject config = resp.jsonObject();
                            if (config.getJsonObject("transformation") != null && config.getJsonObject("transformation").getString("id") != null) {
                                return Future.succeededFuture(config.getJsonObject("transformation").getString("id"));
                            } else {
                                return Future.failedFuture("No transformation ID found in harvestable " + jobConfigId);
                            }
                        } else {
                            return Future.failedFuture("Error retrieving harvestable to get transformation ID: " + resp.errorMessage());
                        }
                    })
                    .compose(transformationId -> TransformationPipeline.create(vertx, tenant, jobConfigId, transformationId))
                    .onComplete(pipelineBuild -> promise.complete(pipelineBuild.result()))
                    .onFailure(e -> Future.failedFuture(e.getMessage()));
        }
        return promise.future();
    }

    public Reporting reporting() {
        return reporting;
    }

    public String getJobConfigId() {
        return jobConfigId;
    }

}