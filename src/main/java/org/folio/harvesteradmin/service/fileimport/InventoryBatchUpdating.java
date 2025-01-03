package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.foliodata.InventoryUpdateClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryBatchUpdating implements RecordReceiver {

    private final JobHandler job;
    private JsonArray inventoryRecordSets = new JsonArray();
    private final BlockingQueue<BatchOfRecords> batchQueue = new ArrayBlockingQueue<>(1);
    private int batchCounter = 0;
    private final InventoryUpdateClient updateClient;

    public InventoryBatchUpdating(JobHandler job, RoutingContext routingContext) {
        updateClient = InventoryUpdateClient.getClient(routingContext);
        this.job = job;
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
                releaseBatch(new BatchOfRecords(records, ++batchCounter, false));
            }
        } else { // a null record is the end-of-file signal, forward remaining records if any
            JsonArray records = inventoryRecordSets.copy();
            inventoryRecordSets = new JsonArray();
            batchCounter += (records.isEmpty() ? 0 : 1); // increment batch count if we got some tail-end records
            releaseBatch(new BatchOfRecords(records, batchCounter, true));
        }
    }

    private void releaseBatch(BatchOfRecords batch) {
        try {
            // add batch to a queue of one (thus wait if a batch is in the queue already)
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

    /**
     * This is the last function of the import pipeline, and since it's asynchronous
     * it must be in charge of when to invoke reporting. JobHandler will not
     * otherwise know when the last upsert of a source file of records is done, for example.
     */
    private void persistBatch() {
        BatchOfRecords batch = batchQueue.peek();
        if (batch != null) {
            if (batch.size() > 0) {
                updateClient.inventoryUpsert(batch.getUpsertRequestBody()).onComplete(json -> {
                    job.reporting().incrementRecordsProcessed(batch.size());
                    job.reporting().incrementInventoryMetrics(new InventoryMetrics(json.result().getJsonObject("metrics")));
                    if (batch.isLastBatchOfFile()) {
                        report(batch);
                    }
                    try {
                        // Open entrance for next batch
                        batchQueue.take();
                    } catch (InterruptedException ignored) {}
                });
            } else { // we get here when the last set of records is exactly 100. We just need to report
                if (batch.isLastBatchOfFile()) {
                    report(batch);
                }
                try {
                    // Open entrance for next batch
                    batchQueue.take();
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void report(BatchOfRecords batch) {
        job.reporting().incrementFilesProcessed();
        job.reporting().reportFileStats();
        var queueDone = job.fileQueueDone(batch.isLastBatchOfFile());
        job.reporting().reportFileQueueStats(queueDone);
        if (queueDone) {
            batchCounter = 0;
        }
    }

    private final AtomicInteger batchQueueIdleChecks = new AtomicInteger(0);
    public boolean noPendingBatches(int idlingChecksThreshold) {
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
