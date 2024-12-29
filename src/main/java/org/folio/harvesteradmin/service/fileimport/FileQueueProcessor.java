package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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

import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;

public class FileQueueProcessor extends AbstractVerticle {

    private FileQueue fileQueue;
    public static final Logger logger = LogManager.getLogger("queued-files-processing");
    private final String tenant;
    private final String jobId;
    private final RoutingContext routingContext;

    public FileQueueProcessor(String tenant, String jobId, RoutingContext routingContext) {
        this.routingContext = routingContext;
        this.tenant = tenant;
        this.jobId = jobId;
    }

    @Override
    public void start() {
        System.out.println("ID-NE: starting file processor for tenant " + tenant + " and job ID " + jobId);
        fileQueue = new FileQueue(vertx, tenant, jobId);
        Reporting reporting = new Reporting(fileQueue);
        vertx.setPeriodic(200, (r) -> {
            InventoryBatchUpdating inventoryBatchUpdating = InventoryBatchUpdating.instance(tenant, jobId, routingContext, reporting, fileQueue);
            File currentFile = nextFileIfPossible(fileQueue, inventoryBatchUpdating);
            if (currentFile != null) {  // null if queue is empty or the previous file is still processing
                reporting.nextFileProcessing(currentFile.getName());
                processFile(jobId, currentFile, inventoryBatchUpdating).onComplete(na -> fileQueue.deleteFile(currentFile))
                        .onFailure(f -> System.out.println("Error processing file: " + f.getMessage()));
            }
        });
    }

    private File nextFileIfPossible(FileQueue fileQueue, InventoryBatchUpdating inventoryBatchUpdating) {
        if (resumeHaltedProcessing(fileQueue, inventoryBatchUpdating)) {
            return fileQueue.currentlyPromotedFile();
        } else if (fileQueue.promoteNextFileIfPossible()) {
            return fileQueue.currentlyPromotedFile();
        }
        return null;
    }

    private boolean resumeHaltedProcessing(FileQueue fileQueue, InventoryBatchUpdating inventoryBatchUpdating) {
        return fileQueue.processingSlotTaken() && inventoryBatchUpdating.batchQueueIdle(10);
    }

    private Future<Void> processFile(String jobId, File xmlFile, InventoryBatchUpdating inventoryUpdater) {
        Promise<Void> promise = Promise.promise();
        try {
            String xmlFileContents = Files.readString(xmlFile.toPath(), StandardCharsets.UTF_8);
            getTransformationPipeline(jobId)
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

    private Future<TransformationPipeline> getTransformationPipeline(String jobId) {
        Promise<TransformationPipeline> promise = Promise.promise();
        if (TransformationPipeline.hasInstance(tenant, jobId)) {
            promise.complete(TransformationPipeline.getInstance(tenant, jobId));
        } else {
            getTransformationId(jobId)
                    .compose(transformationId -> TransformationPipeline.instance(vertx, tenant, jobId, transformationId))
                    .onComplete(pipelineBuild -> promise.complete(pipelineBuild.result()));
        }
        return promise.future();
    }

    private Future<String> getTransformationId(String jobId) {
        Promise<String> promise = Promise.promise();
        LegacyHarvesterStorage legacyHarvesterStorage = new LegacyHarvesterStorage(vertx, tenant);
        legacyHarvesterStorage.getConfigRecordById(HARVESTER_HARVESTABLES_PATH, jobId)
                .onComplete(configResponse -> {
                    if (configResponse.succeeded()) {
                        if (configResponse.result().wasOK()) {
                            JsonObject config = configResponse.result().jsonObject();
                            if (config.getJsonObject("transformation") != null && config.getJsonObject("transformation").getString("id") != null) {
                                promise.complete(config.getJsonObject("transformation").getString("id"));
                            }
                        } else {
                            promise.fail("Could not retrieve harvest job configuration " + configResponse.result().errorMessage());
                        }
                    } else {
                        promise.fail(configResponse.cause().getMessage());
                    }
                });
        return promise.future();
    }

}