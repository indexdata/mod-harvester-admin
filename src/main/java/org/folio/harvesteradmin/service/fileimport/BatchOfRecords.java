package org.folio.harvesteradmin.service.fileimport;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BatchOfRecords {

    private final int batchNumber;
    private final boolean lastBatchOfFile;
    private final JsonObject upsertRequestBody;

    public BatchOfRecords(JsonArray inventoryRecordSets, int batchNumber, boolean lastBatchOfFile) {
        upsertRequestBody = new JsonObject();
        upsertRequestBody.put("inventoryRecordSets", inventoryRecordSets.copy());
        this.batchNumber = batchNumber;
        this.lastBatchOfFile = lastBatchOfFile;
    }

    public boolean isLastBatchOfFile() {
        return lastBatchOfFile;
    }

    public JsonObject getUpsertRequestBody() {
        return upsertRequestBody;
    }

    public int size() {
        return upsertRequestBody.getJsonArray("inventoryRecordSets").size();
    }
}
