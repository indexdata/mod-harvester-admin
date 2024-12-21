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
    private JsonArray inventoryRecordSets = new JsonArray();
    private int batchCounter = 0;
    private final InventoryUpdateClient updateClient;
    private final FileQueue fileQueue;
    private final BlockingQueue<Batch> batchQueue = new ArrayBlockingQueue<>(1);
    private final Reporting reporting;

    private InventoryBatchUpdating(RoutingContext routingContext, Reporting reporting, FileQueue fileQueue) {
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
            inventoryUpdaters.get(tenant).put(jobId, new InventoryBatchUpdating(routingContext, reporting, fileQueue));
        }
        return inventoryUpdaters.get(tenant).get(jobId);
    }

    @Override
    public void put(String jsonRecord) {
        if (jsonRecord != null) {
            JsonObject json = new JsonObject(jsonRecord).getJsonArray("inventoryRecordSets").getJsonObject(0);
            if (!json.containsKey("processing")) {
                json.put("processing", new JsonObject());
            }
            json.getJsonObject("processing").put("batchIndex", inventoryRecordSets.size());
            inventoryRecordSets.add(json);
            if (inventoryRecordSets.size() > 99) {
                JsonArray records = inventoryRecordSets.copy();
                inventoryRecordSets = new JsonArray();
                releaseBatch(new Batch(records, ++batchCounter, false));
            }
        } else { // a null record is the end-of-file signal, forward remaining records if any
            JsonArray records = inventoryRecordSets.copy();
            inventoryRecordSets = new JsonArray();
            batchCounter += (records.isEmpty() ? 0 : 1); // increment batch count if we got some tail-end records
            releaseBatch(new Batch(records, batchCounter, true));
        }
    }

    private void releaseBatch(Batch batch) {
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
            if (batch.size() > 0) {
                // long batchUpsertStarted = System.currentTimeMillis();
                updateClient.inventoryUpsert(batch.getUpsertRequestBody()).onComplete(response -> {
                    reporting.incrementRecordsProcessed(batch.size());
                    // "long batchUpsertTime = System.currentTimeMillis() - batchUpsertStarted;
                    // "System.out.println("Throughput: Job " + jobId + " upserted batch number " + batch.getBatchNumber()
                    // + " with " + batch.size() + " records in " + batchUpsertTime + " ms.");
                    reporting(batch);
                    try {batchQueue.take();} catch (InterruptedException ignored) {}
                });
            } else { // we get here when the last set of records is exactly 100. We just need to report
                reporting(batch);
                try { batchQueue.take();} catch (InterruptedException ignored) {}

            }
        }
    }

    private void reporting(Batch batch) {
        if (batch.isLastBatchOfFile()) {
            reporting.incrementFilesProcessed();
            reporting.reportFileStats();
            if (!fileQueue.hasNextFile()) {
                batchCounter = 0;
                reporting.reportFileQueueStats();
            }
        }
    }

    private final AtomicInteger batchQueueIdleChecks = new AtomicInteger(0);
    public boolean batchQueueIdle(int idlingChecksThreshold) {
        if (batchQueue.isEmpty()) {
            if (batchQueueIdleChecks.incrementAndGet() > idlingChecksThreshold) {
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
