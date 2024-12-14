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
import org.folio.tlib.util.TenantUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;

public class QueuedFilesProcessor extends AbstractVerticle {

    private final FileQueue fileQueue;
    public static final Logger logger = LogManager.getLogger("queued-files-processing");
    private final String tenant;
    private final RoutingContext routingContext;
    private final Map<String,Vertx> vertxInstances = new HashMap<>();

    public QueuedFilesProcessor(Vertx vertx, RoutingContext routingContext) {
        this.routingContext = routingContext;
        this.tenant = TenantUtil.tenant(routingContext);
        fileQueue = new FileQueue(vertx, tenant);
    }

    @Override
    public void start() {
        System.out.println("ID-NE: starting file processor for tenant " + tenant);

        vertx.setPeriodic(2000, (r) -> {
            System.out.println("ID-NE: " + Thread.currentThread().getName());
            for (String jobId : fileQueue.getJobIds()) {
                InventoryBatchUpdating inventoryBatchUpdating = InventoryBatchUpdating.instance(tenant, jobId, routingContext);
                if (fileQueue.couldPromoteNextFile(jobId)) {
                    if (fileQueue.promoteNextFile(jobId)) {
                        String promotedFile = fileQueue.currentlyPromotedFile(jobId);
                        File file = new File(promotedFile);
                        try {
                            String xmlFileContents = Files.readString(new File(promotedFile).toPath(), StandardCharsets.UTF_8);
                            System.out.println("ID-NE " + Thread.currentThread().getName() + " Processing next file " + file.getName() + ", by job ID " + jobId + ", for tenant " + tenant + " next.");
                            processFile(jobId, xmlFileContents, inventoryBatchUpdating).onComplete(na ->
                                    {
                                        fileQueue.deleteFile(file);
                                        if (!fileQueue.hasNextFile(jobId)) {
                                            inventoryBatchUpdating.fileQueueEmpty();
                                            System.out.println("File queue empty, records processed this run: " + inventoryBatchUpdating.getRecordCount());
                                        }
                                    }
                            ).onFailure(f -> System.out.println("Error processing file: " + f.getMessage()));
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                } else {
                    System.out.println("ID-NE, job ID " + jobId + ": Processing busy with " + fileQueue.currentlyPromotedFile(jobId));
                }
            }
        });
    }

    public Future<Void> processFile(String jobId, String xmlFileContents, RecordReceiver inventoryUpdater) {
        Promise<Void> promise = Promise.promise();
        System.out.println("processRecords, thread " + Thread.currentThread().getName());
        getTransformationPipeline(jobId)
                .map(transformationPipeline -> transformationPipeline.setTarget(inventoryUpdater))
                .compose(pipelineToInventory -> vertxInstance(jobId)
                        .executeBlocking(new XmlRecordsFromFile(xmlFileContents).setTarget(pipelineToInventory))
                        .onComplete(transformation -> {
                            if (transformation.succeeded()) {
                                promise.complete();
                            } else {
                                System.out.println("Transformation failed with " + transformation.cause().getMessage());
                                promise.complete();
                            }
                        }));
        return promise.future();
    }

    private Vertx vertxInstance (String jobId) {
        if (! vertxInstances.containsKey(jobId)) {
            vertxInstances.put(jobId, Vertx.vertx());
        }
        return vertxInstances.get(jobId);
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