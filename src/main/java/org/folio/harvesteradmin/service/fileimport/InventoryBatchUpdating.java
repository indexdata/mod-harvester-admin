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
    private long fileQueueStartTime = 0;
    private int filesProcessedThisQueue = 0;
    private long recordsProcessedThisQueue = 0;
    private long batchUpsertTimeThisQueue = 0;
    private long transformationTimeThisQueue = 0;
    private JsonArray inventoryRecordSets = new JsonArray();
    private final AtomicInteger batchSize = new AtomicInteger(0);
    private int batchCounter = 0;
    private final InventoryUpdateClient updateClient;

    private final BlockingQueue<JsonObject> batchQueue = new ArrayBlockingQueue<>(1);
    private InventoryBatchUpdating(RoutingContext routingContext, String jobId) {
        this.jobId = jobId;
        updateClient = InventoryUpdateClient.getClient(routingContext);
    }

    public static InventoryBatchUpdating instance(String tenant, String jobId, RoutingContext routingContext) {
        if (!inventoryUpdaters.containsKey(tenant)) {
            inventoryUpdaters.put(tenant, new HashMap<>());
        }
        if (!inventoryUpdaters.get(tenant).containsKey(jobId)) {
            System.out.println("Creating new instance of InventoryUpdate for tenant '" + tenant + "' job '" + jobId + "'");
            inventoryUpdaters.get(tenant).put(jobId, new InventoryBatchUpdating(routingContext, jobId));
        }
        InventoryBatchUpdating instance = inventoryUpdaters.get(tenant).get(jobId);
        if (instance.fileQueueStartTime==0) {
            instance.fileQueueStartTime=System.currentTimeMillis();
        }
        return instance;
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
                releaseBatch();
            }
        } else {
            System.out.println("Received null record (end-of-document signal)");
            if (batchSize.get()>0) {
                releaseBatch();
            }
        }
    }

    private void releaseBatch() {
        batchCounter++;
        batchSize.set(0);
        JsonObject requestBody = new JsonObject();
        requestBody.put("inventoryRecordSets", inventoryRecordSets.copy());
        inventoryRecordSets = new JsonArray();
        requestBody.put("batchNumber", batchCounter);
        try {
            batchQueue.put(requestBody.copy());
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
        JsonObject batch = batchQueue.peek();
        if (batch != null) {
            long batchUpsertStarted = System.currentTimeMillis();
            updateClient.inventoryUpsert(batch).onComplete(response -> {
                try {
                    long batchUpsertTime = System.currentTimeMillis() - batchUpsertStarted;
                    int records = batch.getJsonArray("inventoryRecordSets").size();
                    batchUpsertTimeThisQueue += batchUpsertTime;
                    recordsProcessedThisQueue += records;
                    System.out.println("Throughput: Job " + jobId + " upserted batch number " + batch.getInteger("batchNumber") + " with " + records + " records in " + batchUpsertTime + " ms.");
                    if ( batchCounter % 100 == 1) {
                        System.out.println("Throughput: Job " + jobId + " processed " + recordsProcessedThisQueue + " records in " + getQueueProcessingTime());
                    }
                    batchQueue.take();
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            });
        }
    }

    public void incrementFilesProcessed () {
        filesProcessedThisQueue++;
    }

    public void incrementTransformationTime(long transformationTime) {
        this.transformationTimeThisQueue += transformationTime;
    }

    public void endOfFileQueue() {
        System.out.println("Throughput: End of queue, job ID " + jobId + "\n" +
         "Throughput: " + filesProcessedThisQueue + " files with " + recordsProcessedThisQueue + " records processed in " + getQueueProcessingTime() +
         " Transformation: " + (transformationTimeThisQueue/recordsProcessedThisQueue) + " ms/rec. Upsert: " +
                 (batchUpsertTimeThisQueue / recordsProcessedThisQueue) + " ms/rec.");
        resetStreamStatistics();
    }

    public void resetStreamStatistics() {
        recordsProcessedThisQueue = 0;
        batchUpsertTimeThisQueue = 0;
        transformationTimeThisQueue = 0;
        fileQueueStartTime=0;
        batchCounter=0;
        filesProcessedThisQueue =0;
    }

    public String getQueueProcessingTime () {
        long processingTime = System.currentTimeMillis()-fileQueueStartTime;
        int hours = (int) processingTime/(1000*60*60);
        long remainingMs = processingTime % (1000*60*60);
        int minutes = (int) remainingMs/(1000*60);
        remainingMs = remainingMs % (1000*60);
        int seconds = (int) remainingMs/1000;
        return (hours>0 ? hours + " hours " : "") +  minutes + " minutes " + seconds + " seconds";
    }

    public int getFilesProcessedThisQueue () {
        return filesProcessedThisQueue;
    }
}
