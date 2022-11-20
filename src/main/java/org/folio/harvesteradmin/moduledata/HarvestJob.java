package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.harvesteradmin.modulestorage.Storage;

public class HarvestJob extends StoredEntity {

  private UUID id;
  private int harvestableId;
  private String name;
  private String type;
  private String url;
  private Boolean allowErrors;
  private Integer recordLimit;
  private Integer batchSize;
  private String transformation;
  private String storage;
  private String status;
  private String started;
  private String finished;
  private Integer amountHarvested;
  private String message;

  private static final String DATE_FORMAT = "YYYY-MM-DD''T''HH24:MI:SS";

  public HarvestJob() {
    super();
  }

  public static HarvestJob entity() {
    return new HarvestJob();
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
    url,
    allow_errors,
    record_limit,
    batch_size,
    transformation,
    storage,
    status,
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
    harvestJob.url = harvestableJson.getString("url");
    harvestJob.allowErrors = Boolean.valueOf(harvestableJson.getString("allowErrors"));
    if (harvestableJson.getString("recordLimit") != null) {
      harvestJob.recordLimit = Integer.parseInt(harvestableJson.getString("recordLimit"));
    }
    if (harvestableJson.getString("storageBatchLimit") != null) {
      harvestJob.batchSize = Integer.parseInt(harvestableJson.getString("storageBatchLimit"));
    }
    harvestJob.transformation = harvestableJson.getJsonObject("transformation").getString("name");
    harvestJob.storage = harvestableJson.getJsonObject("storage").getString("name");
    harvestJob.status = harvestableJson.getString("currentStatus");
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
    json.put("url", url);
    json.put("allowErrors", allowErrors);
    if (recordLimit != null) {
      json.put("recordLimit", recordLimit);
    }
    if (batchSize != null) {
      json.put("batchSize", batchSize);
    }
    json.put("transformation", transformation);
    json.put("storage", storage);
    json.put("status", status);
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
  public String getCreateTableSql(String schema) {
    return "CREATE TABLE IF NOT EXISTS " + schema + "." + Storage.Table.harvest_job
        + "("
        + Column.id + " UUID PRIMARY KEY, "
        + Column.harvestable_id + " INTEGER NOT NULL, "
        + Column.harvestable_name + " TEXT NOT NULL, "
        + Column.type + " TEXT NOT NULL, "
        + Column.url + " TEXT NOT NULL, "
        + Column.allow_errors + " BOOLEAN NOT NULL, "
        + Column.record_limit + " INTEGER, "
        + Column.batch_size + " INTEGER, "
        + Column.transformation + " TEXT NOT NULL, "
        + Column.storage + " TEXT NOT NULL, "
        + Column.status + " TEXT NOT NULL, "
        + Column.started + " TIMESTAMP NOT NULL, "
        + Column.finished + " TIMESTAMP, "
        + Column.amount_harvested + " INTEGER, "
        + Column.message + " TEXT"
        + ")";
  }

  /**
   * INSERT INTO statement.
   */
  public String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.harvest_job
        + " ("
        + Column.id + ", "
        + Column.harvestable_id + ", "
        + Column.harvestable_name + ", "
        + Column.type + ", "
        + Column.url + ", "
        + Column.allow_errors + ", "
        + Column.record_limit + ", "
        + Column.batch_size + ", "
        + Column.transformation + ", "
        + Column.storage + ", "
        + Column.status + ", "
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
        + "#{" + Column.url + "}, "
        + "#{" + Column.allow_errors + "}, "
        + "#{" + Column.record_limit + "}, "
        + "#{" + Column.batch_size + "}, "
        + "#{" + Column.transformation + "}, "
        + "#{" + Column.storage + "}, "
        + "#{" + Column.status + "}, "
        + "TO_TIMESTAMP(#{" + Column.started + "},'" + DATE_FORMAT + "'), "
        + "TO_TIMESTAMP(#{" + Column.finished + "}, '" + DATE_FORMAT + "'), "
        + "#{" + Column.amount_harvested + "}, "
        + "#{" + Column.message + "}"
        + ")";
  }

  /**
   * Table input mapping.
   */
  public TupleMapper<StoredEntity> getInsertValuesMapper() {
    return TupleMapper.mapper(
        harvestJob -> {
          HarvestJob entity = (HarvestJob) harvestJob;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(Column.id.name(), entity.id);
          parameters.put(Column.harvestable_id.name(), entity.harvestableId);
          parameters.put(Column.harvestable_name.name(), entity.name);
          parameters.put(Column.type.name(), entity.type);
          parameters.put(Column.url.name(), entity.url);
          parameters.put(Column.allow_errors.name(), entity.allowErrors);
          if (entity.recordLimit != null) {
            parameters.put(Column.record_limit.name(), entity.recordLimit);
          }
          if (entity.batchSize != null) {
            parameters.put(Column.batch_size.name(), entity.batchSize);
          }
          parameters.put(Column.transformation.name(), entity.transformation);
          parameters.put(Column.storage.name(), entity.storage);
          parameters.put(Column.status.name(), entity.status);
          parameters.put(Column.started.name(), entity.started);
          parameters.put(Column.finished.name(), entity.finished);
          if (entity.amountHarvested != null) {
            parameters.put(Column.amount_harvested.name(), entity.amountHarvested);
          }
          parameters.put(Column.message.name(), entity.message);
          return parameters;
        });
  }

  /**
   * Table output mapping.
   */
  public RowMapper<StoredEntity> getSelectListMapper() {
    return row -> {
      HarvestJob harvestJob = new HarvestJob();
      harvestJob.id = row.getUUID(Column.id.name());
      harvestJob.name = row.getString(Column.harvestable_name.name());
      harvestJob.harvestableId = row.getInteger(Column.harvestable_id.name());
      harvestJob.type = row.getString(Column.type.name());
      harvestJob.url = row.getString(Column.url.name());
      harvestJob.allowErrors = row.getBoolean(Column.allow_errors.name());
      if (row.getValue(Column.record_limit.name()) != null) {
        harvestJob.recordLimit = row.getInteger(Column.record_limit.name());
      }
      harvestJob.transformation = row.getString(Column.transformation.name());
      harvestJob.storage = row.getString(Column.storage.name());
      harvestJob.status = row.getString(Column.status.name());
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
