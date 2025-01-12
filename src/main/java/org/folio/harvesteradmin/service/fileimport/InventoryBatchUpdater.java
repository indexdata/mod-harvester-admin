package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.foliodata.InventoryUpdateClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryBatchUpdater implements RecordReceiver {

    private final XmlFilesImporter job;
    private JsonArray inventoryRecordSets = new JsonArray();
    private int batchCounter = 0;
    private final InventoryUpdateClient updateClient;

    private final Turnstile turnstile = new Turnstile();

    public InventoryBatchUpdater(XmlFilesImporter job, RoutingContext routingContext) {
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
        turnstile.enterBatch(batch);
        persistBatch().onComplete(na -> turnstile.exitBatch());
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
    private Future<Void> persistBatch() {
        Promise<Void> promise = Promise.promise();
        BatchOfRecords batch = turnstile.viewCurrentBatch();
        if (batch != null) {
            if (batch.size() > 0) {
                updateClient.inventoryUpsert(batch.getUpsertRequestBody()).onComplete(json -> {
                    job.reporting().incrementRecordsProcessed(batch.size());
                    job.reporting().incrementInventoryMetrics(new InventoryMetrics(json.result().getJsonObject("metrics")));
                    if (batch.isLastBatchOfFile()) {
                        report(batch);
                    }
                    promise.complete();
                });
            } else { // we get here when the last set of records is exactly 100. We just need to report
                if (batch.isLastBatchOfFile()) {
                    report(batch);
                }
                promise.complete();
            }
        }
        return promise.future();
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

    public boolean noPendingBatches(int idlingChecksThreshold) {
        return turnstile.isIdle(idlingChecksThreshold);
    }


    /** Class wrapping a blocking queue of one, acting as a turnstile for batches in order to persist them one
     * at a time with no overlap. */
    private static class Turnstile {

        private final BlockingQueue<BatchOfRecords> turnstile = new ArrayBlockingQueue<>(1);
        // Records the number of consecutive checks of whether the queue is idling.
        private final AtomicInteger turnstileEmptyChecks = new AtomicInteger(0);

        /**
         * Puts batch in blocking queue-of-one; process waits if previous batch still in queue.
         */
        private void enterBatch(BatchOfRecords batch) {
            try {
                turnstile.put(batch);
            } catch (InterruptedException ie) {
                throw new RuntimeException("Putting next batch in queue-of-one interrupted: " + ie.getMessage());
            }
        }

        private void exitBatch() {
            try {
                turnstile.take();
            } catch (InterruptedException ie) {
                throw new RuntimeException("Taking batch from queue-of-one interrupted: " + ie.getMessage());
            }
        }

        private BatchOfRecords viewCurrentBatch() {
            return turnstile.peek();
        }

        private boolean isIdle(int idlingChecksThreshold) {
            if (turnstile.isEmpty()) {
                if (turnstileEmptyChecks.incrementAndGet() > idlingChecksThreshold) {
                    System.out.println("ID-NE: Turnstile has been idle for " + idlingChecksThreshold + " consecutive checks.");
                    turnstileEmptyChecks.set(0);
                    return true;
                }
            } else {
                turnstileEmptyChecks.set(0);
            }
            return false;
        }
   }
}
