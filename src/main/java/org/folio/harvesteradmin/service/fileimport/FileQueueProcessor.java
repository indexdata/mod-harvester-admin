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

import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;

public class FileQueueProcessor extends AbstractVerticle {

    private final FileQueue fileQueue;
    public static final Logger logger = LogManager.getLogger("queued-files-processing");
    private final String tenant;
    private final String jobId;
    private final RoutingContext routingContext;

    public FileQueueProcessor(Vertx vertx, String tenant, String jobId, RoutingContext routingContext) {
        this.routingContext = routingContext;
        this.tenant = tenant;
        this.jobId = jobId;
        fileQueue = new FileQueue(vertx, tenant, jobId);
    }

    @Override
    public void start() {
        System.out.println("ID-NE: starting file processor for tenant " + tenant + " and job ID " + jobId);

        vertx.setPeriodic(100, (r) -> {
            if (fileQueue.couldPromoteNextFile()) {
                if (fileQueue.promoteNextFile()) {
                    InventoryBatchUpdating inventoryBatchUpdating = InventoryBatchUpdating.instance(tenant, jobId, routingContext);
                    String promotedFile = fileQueue.currentlyPromotedFile();
                    File file = new File(promotedFile);
                    try {
                        String xmlFileContents = Files.readString(new File(promotedFile).toPath(), StandardCharsets.UTF_8);
                        long fileProcessStarted = System.currentTimeMillis();
                        processFile(jobId, xmlFileContents, inventoryBatchUpdating).onComplete(na ->
                                {
                                    System.out.println("Throughput: file number " + inventoryBatchUpdating.getFilesProcessedThisQueue() + " processed in " + (System.currentTimeMillis()-fileProcessStarted) + " ms.,  " + fileQueue.filesInQueue() + " more files in queue. Processed file: " + file.getName() + ". Job " + jobId);
                                    fileQueue.deleteFile(file);
                                    if (!fileQueue.hasNextFile()) {
                                        inventoryBatchUpdating.endOfFileQueue();
                                    }
                                }
                        ).onFailure(f -> System.out.println("Error processing file: " + f.getMessage()));
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });
    }

    public Future<Void> processFile(String jobId, String xmlFileContents, InventoryBatchUpdating inventoryUpdater) {
        Promise<Void> promise = Promise.promise();
        inventoryUpdater.incrementFilesProcessed();
        getTransformationPipeline(jobId)
                .map(transformationPipeline -> transformationPipeline.setTarget(inventoryUpdater))
                .compose(pipelineToInventory -> vertx
                        .executeBlocking(new XmlRecordsFromFile(xmlFileContents).setTarget(pipelineToInventory))
                        .onComplete(processing -> {
                            if (processing.succeeded()) {
                                inventoryUpdater.incrementTransformationTime(pipelineToInventory.transformationTime());
                                promise.complete();
                            } else {
                                System.out.println("Processing failed with " + processing.cause().getMessage());
                                promise.complete();
                            }
                        }));
        return promise.future();
    }

    private Future<TransformationPipeline> getTransformationPipeline (String jobId) {
        Promise<TransformationPipeline> promise = Promise.promise();
        if (TransformationPipeline.hasInstance(tenant, jobId)) {
            promise.complete(TransformationPipeline.getInstance(tenant,jobId));
        } else {
            getTransformationId(jobId)
                    .compose(transformationId -> TransformationPipeline.instance(vertx, tenant, jobId, transformationId))
                    .onComplete(pipelineBuild -> promise.complete(pipelineBuild.result()));
        }
        return promise.future();
    }

    private Future<String> getTransformationId(String jobId) {
        Promise<String> promise = Promise.promise();
        LegacyHarvesterStorage legacyHarvesterStorage = new LegacyHarvesterStorage(vertx,tenant);
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