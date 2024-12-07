package org.folio.harvesteradmin.service.harvest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.harvesteradmin.service.harvest.transformation.RecordReceiver;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.lang.Thread.sleep;


public class InventoryUpdate implements RecordReceiver {

    private static final Map<String, Map<String, InventoryUpdate>> inventoryUpdaters = new HashMap<>();
    private long recordsProcessedThisQueue = 0;
    private JsonArray inventoryRecordSets = new JsonArray();
    private int batchSize = 0;
    private int batchCounter = 0;
    private final Vertx vertx;

    private final BlockingQueue<JsonObject> batchQueue = new ArrayBlockingQueue<>(1);
    private InventoryUpdate() {
        vertx = Vertx.vertx();
        System.out.println("Initiated Vertx instance: " + vertx);
    }

    public static InventoryUpdate instance(String tenant, String jobId) {
        if (!inventoryUpdaters.containsKey(tenant)) {
            inventoryUpdaters.put(tenant, new HashMap<>());
        }
        if (!inventoryUpdaters.get(tenant).containsKey(jobId)) {
            System.out.println("Creating new instance of InventoryUpdate for tenant '" + tenant + "' job '" + jobId + "'");
            inventoryUpdaters.get(tenant).put(jobId, new InventoryUpdate());
        }
        return inventoryUpdaters.get(tenant).get(jobId);
    }

    @Override
    public void put(String jsonRecord) {
        if (jsonRecord != null) {
            recordsProcessedThisQueue++;
            inventoryRecordSets.add(jsonRecord);
            batchSize++;
            if (batchSize>99) {
                handlePopulatedBatch();
            }
        } else {
            System.out.println("Received null record (end-of-document signal)");
            if (batchSize>0) {
                handlePopulatedBatch();
            }
        }
    }

    private void handlePopulatedBatch() {
        batchCounter++;
        batchSize = 0;
        JsonObject requestBody = new JsonObject();
        requestBody.put("inventoryRecordSets", inventoryRecordSets.copy());
        inventoryRecordSets = new JsonArray();
        requestBody.put("batchNumber", batchCounter);
        System.out.println("Adding batch " + requestBody.getInteger("batchNumber") + " to batches for update.");
        try {
            batchQueue.put(requestBody.copy());
            persistBatch();
        } catch (InterruptedException ie) {
            System.out.println("Error: Queue put operation was interrupted.");
        }
        System.out.println("batchList has " + batchQueue.size() + " batches.");
    }

    @Override
    public void endOfDocument() {
        put(null);
    }

    private void persistBatch() {
        System.out.println("In persistBatch(), got list of " + batchQueue.size() + " batches in blocking queue. ");
        vertx.executeBlocking(future -> {
            try {
                JsonObject batch = batchQueue.peek();
                System.out.println("sending JSON start, batchNumber " + batch.getInteger("batchNumber") + " records: " + batch.getJsonArray("inventoryRecordSets").size());
                sleep(1000);
                System.out.println("Sending JSON end " + batch.getInteger("batchNumber"));
                batchQueue.take();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }, true);
    }

    public long getRecordCount() {
        return recordsProcessedThisQueue;
    }

    public void fileQueueEmpty() {
        System.out.println("fileQueueEmpty(): File queue is empty.");
        batchCounter = 0;
        resetStreamStatistics();
    }

    public void resetStreamStatistics() {
        recordsProcessedThisQueue = 0;
    }
}
