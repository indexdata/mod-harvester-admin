package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.foliodata.InventoryUpdateClient;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;


public class InventoryBatchUpdating implements RecordReceiver {

    private static final Map<String, Map<String, InventoryBatchUpdating>> inventoryUpdaters = new HashMap<>();
    private long recordsProcessedThisQueue = 0;
    private JsonObject batch = new JsonObject();
    private JsonArray inventoryRecordSets = new JsonArray();
    private final AtomicInteger batchSize = new AtomicInteger(0);
    private int batchCounter = 0;
    private final Vertx vertx;
    private final RoutingContext routingContext;
    private final InventoryUpdateClient updateClient;

    private final BlockingQueue<JsonObject> batchQueue = new ArrayBlockingQueue<>(1);
    private InventoryBatchUpdating(RoutingContext routingContext) {
        vertx = Vertx.vertx();
        this.routingContext = routingContext;
        updateClient = InventoryUpdateClient.getClient(routingContext);
        System.out.println("Initiated Vertx instance: " + vertx);
    }

    public static InventoryBatchUpdating instance(String tenant, String jobId, RoutingContext routingContext) {
        if (!inventoryUpdaters.containsKey(tenant)) {
            inventoryUpdaters.put(tenant, new HashMap<>());
        }
        if (!inventoryUpdaters.get(tenant).containsKey(jobId)) {
            System.out.println("Creating new instance of InventoryUpdate for tenant '" + tenant + "' job '" + jobId + "'");
            inventoryUpdaters.get(tenant).put(jobId, new InventoryBatchUpdating(routingContext));
        }
        return inventoryUpdaters.get(tenant).get(jobId);
    }

    @Override
    public void put(String jsonRecord) {
        if (jsonRecord != null) {
            recordsProcessedThisQueue++;
            inventoryRecordSets.add(new JsonObject(jsonRecord).getJsonArray("inventoryRecordSets").getJsonObject(0));
            if (batchSize.incrementAndGet()>99) {
                handlePopulatedBatch();
            }
        } else {
            System.out.println("Received null record (end-of-document signal)");
            if (batchSize.get()>0) {
                handlePopulatedBatch();
            }
        }
    }

    private void handlePopulatedBatch() {
        batchCounter++;
        batchSize.set(0);
        JsonObject requestBody = new JsonObject();
        requestBody.put("inventoryRecordSets", inventoryRecordSets.copy());
        inventoryRecordSets = new JsonArray();
        requestBody.put("batchNumber", batchCounter);
        try {
            batchQueue.put(requestBody.copy());
            persistBatch(routingContext);
        } catch (InterruptedException ie) {
            System.out.println("Error: Queue put operation was interrupted.");
        }
    }

    @Override
    public void endOfDocument() {
        put(null);
    }

    private void persistBatch(RoutingContext routingContext) {
        vertx.executeBlocking(promise -> {
            JsonObject batch = batchQueue.peek();
            if (batch != null) {
                System.out.println("Upserting batch number " + batch.getInteger("batchNumber") + ".");
                updateClient.inventoryUpsert(routingContext, batch).onComplete(response -> {
                    try {
                        batchQueue.take();
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                });
            }
        }, true);
    }

    public long getRecordCount() {
        return recordsProcessedThisQueue;
    }

    public void fileQueueEmpty() {
        System.out.println("fileQueueEmpty(): File queue is empty. Records processed this run: " + recordsProcessedThisQueue );
        resetStreamStatistics();
    }

    public void resetStreamStatistics() {
        recordsProcessedThisQueue = 0;
        batchCounter=0;
    }


}
