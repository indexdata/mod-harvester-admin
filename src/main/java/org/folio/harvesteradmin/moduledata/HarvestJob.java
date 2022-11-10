package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.harvesteradmin.modulestorage.Storage;

public class HarvestJob {

  private UUID id;
  private int harvestableId;
  private String name;
  private String type;
  private Boolean allowErrors;
  private String started;
  private String finished;
  private Integer recordLimit;
  private Integer amountHarvested;
  private String message;

  public HarvestJob() {
    super();
  }

  public UUID id() {
    return id;
  }

  public String name() {
    return name;
  }

  /**
   * HARVEST_JOB columns.
   */
  public enum Column {
    id,
    harvestable_id,
    harvestable_name,
    allow_errors,
    record_limit,
    started,
    finished,
    amount_harvested,
    message,
    type
  }

  /**
   * Maps legacy harvestable JSON to HarvestJob Java Object.
   */
  public static HarvestJob fromHarvestableJson(JsonObject harvestableJson) {
    HarvestJob harvestJob = new HarvestJob();
    harvestJob.id = UUID.randomUUID();
    harvestJob.harvestableId = Integer.parseInt(harvestableJson.getString("id"));
    harvestJob.name = harvestableJson.getString("name");
    harvestJob.type = harvestableJson.getString("type");
    harvestJob.allowErrors = Boolean.valueOf(harvestableJson.getString("allowErrors"));
    if (harvestableJson.getString("recordLimit") != null) {
      harvestJob.recordLimit = Integer.parseInt(harvestableJson.getString("recordLimit"));
    }
    harvestJob.started = harvestableJson.getString("lastHarvestStarted");
    if (harvestableJson.getString("lastHarvestStarted")
        .compareTo(harvestableJson.getString("lastHarvestFinished")) < 0) {
      harvestJob.finished = harvestableJson.getString("lastHarvestFinished");
    }
    if (harvestableJson.getString("amountHarvested") != null) {
      harvestJob.amountHarvested = Integer.parseInt(harvestableJson.getString("amountHarvested"));
    }
    harvestJob.message = harvestableJson.getString("message");
    return harvestJob;
  }

  /**
   * HarvestJob to JSON mapping.
   */
  public JsonObject asJson() {
    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("name", name);
    json.put("harvestableId", harvestableId);
    json.put("type", type);
    json.put("allowErrors", allowErrors);
    if (recordLimit != null) {
      json.put("recordLimit", recordLimit);
    }
    json.put("started", started);
    json.put("finished", finished);
    if (amountHarvested != null) {
      json.put("amountHarvested", amountHarvested);
    }
    json.put("message", message);
    return json;
  }

  /**
   * CREATE TABLE statement.
   */
  public static String getCreateTableSql(String schema) {
    return "CREATE TABLE IF NOT EXISTS " + schema + "." + Storage.Table.harvest_job
        + "("
        + Column.id + " UUID PRIMARY KEY, "
        + Column.harvestable_id + " INTEGER NOT NULL, "
        + Column.harvestable_name + " TEXT NOT NULL, "
        + Column.type + " TEXT NOT NULL, "
        + Column.allow_errors + " BOOLEAN NOT NULL, "
        + Column.record_limit + " INTEGER, "
        + Column.started + " TIMESTAMP NOT NULL, "
        + Column.finished + " TIMESTAMP, "
        + Column.amount_harvested + " INTEGER, "
        + Column.message + " TEXT"
        + ")";
  }

  /**
   * INSERT INTO statement.
   */
  public static String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.harvest_job
        + " ("
        + Column.id + ", "
        + Column.harvestable_id + ", "
        + Column.harvestable_name + ", "
        + Column.type + ", "
        + Column.allow_errors + ", "
        + Column.record_limit + ", "
        + Column.started + ", "
        + Column.finished + ", "
        + Column.amount_harvested + ", "
        + Column.message
        + ")"
        + " VALUES ("
        + "#{" + Column.id + "}, "
        + "#{" + Column.harvestable_id + "}, "
        + "#{" + Column.harvestable_name + "}, "
        + "#{" + Column.type + "}, "
        + "#{" + Column.allow_errors + "}, "
        + "#{" + Column.record_limit + "}, "
        + "TO_TIMESTAMP(#{" + Column.started + "},'YYYY-MM-DD''T''HH24:MI:SS''Z'''), "
        + "TO_TIMESTAMP(#{" + Column.finished + "}, 'YYYY-MM-DD''T''HH24:MI:SS''Z'''), "
        + "#{" + Column.amount_harvested + "}, "
        + "#{" + Column.message + "}"
        + ")";
  }

  /**
   * Table input mapping.
   */
  public static TupleMapper<HarvestJob> getInsertValuesMapper() {
    return TupleMapper.mapper(
        harvestJob -> {
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(Column.id.name(), harvestJob.id);
          parameters.put(Column.harvestable_id.name(), harvestJob.harvestableId);
          parameters.put(Column.harvestable_name.name(), harvestJob.name);
          parameters.put(Column.type.name(), harvestJob.type);
          parameters.put(Column.allow_errors.name(), harvestJob.allowErrors);
          if (harvestJob.recordLimit != null) {
            parameters.put(Column.record_limit.name(), harvestJob.recordLimit);
          }
          parameters.put(Column.started.name(), harvestJob.started);
          parameters.put(Column.finished.name(), harvestJob.finished);
          if (harvestJob.amountHarvested != null) {
            parameters.put(Column.amount_harvested.name(), harvestJob.amountHarvested);
          }
          parameters.put(Column.message.name(), harvestJob.message);
          return parameters;
        });
  }

  /**
   * Table output mapping.
   */
  public static RowMapper<HarvestJob> getSelectListMapper() {
    return row -> {
      HarvestJob harvestJob = new HarvestJob();
      harvestJob.id = row.getUUID(Column.id.name());
      harvestJob.name = row.getString(Column.harvestable_name.name());
      harvestJob.harvestableId = row.getInteger(Column.harvestable_id.name());
      harvestJob.type = row.getString(Column.type.name());
      harvestJob.allowErrors = row.getBoolean(Column.allow_errors.name());
      if (row.getValue(Column.record_limit.name()) != null) {
        harvestJob.recordLimit = row.getInteger(Column.record_limit.name());
      }
      if (row.getValue(Column.started.name()) != null) {
        harvestJob.started = row.getLocalDateTime(Column.started.name()).toString();
      }
      if (row.getValue(Column.finished.name()) != null) {
        harvestJob.finished = row.getLocalDateTime(Column.finished.name()).toString();
      }
      if (row.getValue(Column.amount_harvested.name()) != null) {
        harvestJob.amountHarvested = row.getInteger(Column.amount_harvested.name());
      }
      harvestJob.message = row.getString(Column.message.name());
      return harvestJob;
    };
  }


}
