package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.harvesteradmin.modulestorage.Storage;

public class RecordFailure extends StoredEntity {

  private UUID id;
  private UUID harvestJobId;
  private String originalRecord;
  private JsonArray recordErrors;
  private JsonObject transformedRecord;

  public enum Column {
    id,
    harvest_job_id,
    record_errors,
    original_record,
    transformed_record
  }

  public static RecordFailure entity() {
    return new RecordFailure();
  }

  /**
   * Constructor.
   */
  public static RecordFailure fromLegacyHarvesterJson(UUID harvestJobId, JsonObject json) {
    RecordFailure recordFailure = new RecordFailure();
    recordFailure.id = UUID.randomUUID();
    recordFailure.harvestJobId = harvestJobId;
    recordFailure.originalRecord = json.getString("original");
    recordFailure.transformedRecord = json.getJsonObject("transformed-record");
    recordFailure.recordErrors = json.getJsonArray("record-errors");
    return recordFailure;
  }

  /**
   * CREATE TABLE statement.
   */
  public String getCreateTableSql(String schema) {
    return "CREATE TABLE IF NOT EXISTS " + schema + "." + Storage.Table.record_failure
        + "("
        + Column.id + " UUID PRIMARY KEY, "
        + Column.harvest_job_id + " UUID NOT NULL REFERENCES "
        +         schema + "." + Storage.Table.harvest_job + "(" + HarvestJob.Column.id + "), "
        + Column.record_errors + " JSONB NOT NULL, "
        + Column.original_record + " TEXT NOT NULL, "
        + Column.transformed_record + " JSONB NOT NULL"
        + ")";
  }

  @Override
  public RowMapper<StoredEntity> getSelectListMapper() {
    return row -> {
      RecordFailure recordFailure = new RecordFailure();
      recordFailure.id = row.getUUID(RecordFailure.Column.id.name());
      recordFailure.harvestJobId = row.getUUID(Column.harvest_job_id.name());
      recordFailure.recordErrors = row.getJsonArray(Column.record_errors.name());
      recordFailure.originalRecord = row.getString(Column.original_record.name());
      recordFailure.transformedRecord = row.getJsonObject(Column.transformed_record.name());
      return recordFailure;
    };
  }

  @Override
  public TupleMapper<StoredEntity> getInsertValuesMapper() {
    return TupleMapper.mapper(
        recordFailure -> {
          RecordFailure entity = (RecordFailure) recordFailure;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(RecordFailure.Column.id.name(), entity.id);
          parameters.put(Column.harvest_job_id.name(), entity.harvestJobId);
          parameters.put(Column.original_record.name(), entity.originalRecord);
          parameters.put(Column.transformed_record.name(), entity.transformedRecord);
          parameters.put(RecordFailure.Column.record_errors.name(), entity.recordErrors);
          return parameters;
        });
  }

  @Override
  public String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.record_failure
        + " ("
        + Column.id + ", "
        + Column.harvest_job_id + ", "
        + Column.record_errors + ", "
        + Column.original_record + ", "
        + Column.transformed_record
        + ")"
        + " VALUES ("
        + "#{" + Column.id + "}, "
        + "#{" + Column.harvest_job_id + "}, "
        + "#{" + Column.record_errors + "}, "
        + "#{" + Column.original_record + "}, "
        + "#{" + Column.transformed_record + "}"
        + ")";
  }

  /**
   * Gets JSON representation.
   */
  public JsonObject asJson() {
    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("harvestJobId", harvestJobId);
    json.put("recordErrors", recordErrors);
    json.put("originalRecord", originalRecord);
    json.put("transformedRecord", transformedRecord);
    return json;
  }

}

