package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.foliodata.InventoryUpdateClient;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class InventoryBatchUpdating implements RecordReceiver {

    private static final Map<String, Map<String, InventoryBatchUpdating>> inventoryUpdaters = new HashMap<>();
    private final String jobId;
    private long batchUpsertTimeThisQueue = 0;
    private long transformationTimeThisQueue = 0;
    private JsonArray inventoryRecordSets = new JsonArray();
    private final AtomicInteger batchSize = new AtomicInteger(0);
    private int batchCounter = 0;
    private final InventoryUpdateClient updateClient;
    private final FileQueue fileQueue;

    private final BlockingQueue<Batch> batchQueue = new ArrayBlockingQueue<>(1);
    private final AtomicInteger batchQueueIdleChecks = new AtomicInteger(0);
    private final Reporting reporting;
    private InventoryBatchUpdating(RoutingContext routingContext, String jobId, Reporting reporting, FileQueue fileQueue) {
        this.jobId = jobId;
        updateClient = InventoryUpdateClient.getClient(routingContext);
        this.reporting = reporting;
        this.fileQueue = fileQueue;
    }

    public static InventoryBatchUpdating instance(String tenant, String jobId, RoutingContext routingContext, Reporting reporting, FileQueue fileQueue) {
        if (!inventoryUpdaters.containsKey(tenant)) {
            inventoryUpdaters.put(tenant, new HashMap<>());
        }
        if (!inventoryUpdaters.get(tenant).containsKey(jobId)) {
            System.out.println("Creating new instance of InventoryBatchUpdating for tenant '" + tenant + "' job '" + jobId + "'");
            inventoryUpdaters.get(tenant).put(jobId, new InventoryBatchUpdating(routingContext, jobId, reporting, fileQueue));
        } else {
            // System.out.println("Reusing existing instance of InventoryBatchUpdating");
        }
        return inventoryUpdaters.get(tenant).get(jobId);
    }

    @Override
    public void put(String jsonRecord) {
        if (jsonRecord != null) {
            JsonObject json = new JsonObject(jsonRecord).getJsonArray("inventoryRecordSets").getJsonObject(0);
            if (!json.containsKey("processing")) {
                json.put("processing",new JsonObject());
            }
            json.getJsonObject("processing").put("batchIndex", batchSize);
            inventoryRecordSets.add(json);
            if (batchSize.incrementAndGet()>99) {
                JsonArray records = inventoryRecordSets.copy();
                inventoryRecordSets = new JsonArray();
                releaseBatch(new Batch(records, ++batchCounter, false));
            }
        } else {
            //System.out.println("Received null record (end-of-document signal). Last batch count was " + batchCounter);
            JsonArray records = inventoryRecordSets.copy();
            inventoryRecordSets = new JsonArray();
            if (!records.isEmpty()) batchCounter++;
            releaseBatch(new Batch(records, batchCounter, true));
        }
    }

    private void releaseBatch(Batch batch) {
        batchSize.set(0);
         try {
            batchQueue.put(batch);
            persistBatch();
        } catch (InterruptedException ie) {
            System.out.println("Error: Queue put operation was interrupted.");
        }
    }

    @Override
    public void endOfDocument() {
        put(null);
    }

    private void persistBatch() {
        Batch batch = batchQueue.peek();
        if (batch != null) {
            if (batch.size()>0) {
                long batchUpsertStarted = System.currentTimeMillis();
                updateClient.inventoryUpsert(batch.getUpsertRequestBody()).onComplete(response -> {
                    try {
                        reporting.incrementRecordsProcessed(batch.size());
                        long batchUpsertTime = System.currentTimeMillis() - batchUpsertStarted;
                        batchUpsertTimeThisQueue += batchUpsertTime;
                        System.out.println("Throughput: Job " + jobId + " upserted batch number " + batch.getBatchNumber() + " with " + batch.size() + " records in " + batchUpsertTime + " ms.");
                        if (batch.isLastBatchOfFile()) {
                            reporting.incrementFilesProcessed();
                            reporting.reportFileStats();
                            if (!fileQueue.hasNextFile()) {
                                batchCounter = 0;
                                reporting.reportFileQueueStats();
                            }
                        }
                        batchQueue.take();
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted: " + e.getMessage());
                    }
                });
            } else {
                try {
                    if (batch.isLastBatchOfFile()) {
                        reporting.incrementFilesProcessed();
                        reporting.reportFileStats();
                    }
                    batchQueue.take();
                    if (!fileQueue.hasNextFile()) {
                        batchCounter = 0;
                        reporting.reportFileQueueStats();
                    }
                } catch (InterruptedException ignored) {}

            }
        }
    }

    public void incrementTransformationTime(long transformationTime) {
        this.transformationTimeThisQueue += transformationTime;
    }

    public boolean batchQueueIdle(int idlingChecksThreshold) {
        if (batchQueue.isEmpty()) {
            if (batchQueueIdleChecks.incrementAndGet()>idlingChecksThreshold) {
                System.out.println("ID-NE: BatchQueue has been idle for " + idlingChecksThreshold + " consecutive checks.");
                batchQueueIdleChecks.set(0);
                return true;
            }
        } else {
            batchQueueIdleChecks.set(0);
        }
        return false;
    }
}
