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
            //System.out.println("releaseBatch() # "+batch.getBatchNumber() +" with " + batch.size() + " records, batch queue size before put: " + batchQueue.size());
            batchQueue.put(batch);
            //System.out.println("releaseBatch() # "+batch.getBatchNumber() +" with " + batch.size() + ", batch queue size after put: " + batchQueue.size());
            persistBatch();
        } catch (InterruptedException ie) {
            System.out.println("Error: Queue put operation was interrupted.");
        }
    }

    @Override
    public void endOfDocument() {
        //System.out.println("endOfDocument() putting null record ");
        put(null);
        //System.out.println("endOfDocument() null record put");
    }

    private void persistBatch() {
        BatchOfRecords batch = batchQueue.peek();
        //System.out.println("persistBatch, peeked batch #" + (batch == null ? " null " : batch.getBatchNumber() + " with " + batch.size() + "records, last batch? " + batch.isLastBatchOfFile()));
        if (batch != null) {
            if (batch.size() > 0) {
                // long batchUpsertStarted = System.currentTimeMillis();
                updateClient.inventoryUpsert(batch.getUpsertRequestBody()).onComplete(response -> {
                    job.reporting().incrementRecordsProcessed(batch.size());
                    //System.out.println("persistBatch(), upsert done for batch #" + batch.getBatchNumber() + " with " + batch.size() + " records");
                    reporting(batch);
                    try {
                        //System.out.println("Taking batch from queue with " + batchQueue.size() + " batches");
                        batchQueue.take();
                        //System.out.println("Took batch #" + takebatch.getBatchNumber() + " from queue, last batch? " + takebatch.isLastBatchOfFile());

                    } catch (InterruptedException ignored) {}
                });
            } else { // we get here when the last set of records is exactly 100. We just need to report
                //System.out.println("persistBatch(), batch #" + batch.getBatchNumber() + " with " + batch.size() + " records, is last batch? " + batch.isLastBatchOfFile());
                reporting(batch);
                try {
                    //System.out.println("Taking batch from queue with " + batchQueue.size() + " batches");
                    batchQueue.take();
                    //System.out.println("Took batch #" + takebatch.getBatchNumber() + " from queue, last batch? " + takebatch.isLastBatchOfFile());
                } catch (InterruptedException ignored) {}

            }
        }
    }

    private void reporting(BatchOfRecords batch) {
        //System.out.println("Report if last batch, last batch? " + batch.isLastBatchOfFile());
        if (batch.isLastBatchOfFile()) {
            job.reporting().incrementFilesProcessed();
            job.reporting().reportFileStats();
            job.reporting().reportFileQueueStats();
            if (job.reporting().fileQueueDone()) {
                //System.out.println("reporting() no more files in queue, no pending file stats, reset batch counter");
                batchCounter = 0;
            }
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
