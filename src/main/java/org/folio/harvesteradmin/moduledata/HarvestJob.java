package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.harvesteradmin.modulestorage.HarvestAdminStorage.HarvestJobTable;


public class HarvestJob extends Entity {

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

  public HarvestJob(JsonObject json) {
    super(json);
  }

  public HarvestJob() {
    super();
  }

  /**
   * Maps harvestable JSON to HarvestJob Java Object.
   */
  public static HarvestJob fromHarvestableJson(JsonObject json) {
    HarvestJob harvestJob = new HarvestJob(json);
    harvestJob.id = harvestJob.uuid;
    harvestJob.harvestableId = Integer.parseInt(json.getString("id"));
    harvestJob.name = json.getString("name");
    harvestJob.type = json.getString("type");
    harvestJob.allowErrors = Boolean.valueOf(json.getString("allowErrors"));
    if (json.getString("recordLimit") != null) {
      harvestJob.recordLimit = Integer.parseInt(json.getString("recordLimit"));
    }
    harvestJob.started = json.getString("lastHarvestStarted");
    harvestJob.finished = json.getString("lastHarvestFinished");
    if (json.getString("amountHarvested") != null) {
      harvestJob.amountHarvested = Integer.parseInt(json.getString("amountHarvested"));
    }
    harvestJob.message = json.getString("message");
    return harvestJob;
  }

  /**
   * Creates a TupleMapper for input mapping.
   */
  public static TupleMapper<HarvestJob> tupleMapper() {
    return TupleMapper.mapper(
        harvestJob -> {
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(HarvestJobTable.id.name(), harvestJob.id);
          parameters.put(HarvestJobTable.harvestable_id.name(), harvestJob.harvestableId);
          parameters.put(HarvestJobTable.harvestable_name.name(), harvestJob.name);
          parameters.put(HarvestJobTable.type.name(), harvestJob.type);
          parameters.put(HarvestJobTable.allow_errors.name(), harvestJob.allowErrors);
          if (harvestJob.recordLimit != null) {
            parameters.put(HarvestJobTable.record_limit.name(), harvestJob.recordLimit);
          }
          parameters.put(HarvestJobTable.started.name(), harvestJob.started);
          parameters.put(HarvestJobTable.finished.name(), harvestJob.finished);
          if (harvestJob.amountHarvested != null) {
            parameters.put(HarvestJobTable.amount_harvested.name(), harvestJob.amountHarvested);
          }
          parameters.put(HarvestJobTable.message.name(), harvestJob.message);
          return parameters;
        });
  }

  /**
   * Creates a RowMapper for output mapping.
   */
  public static RowMapper<HarvestJob> rowMapper() {
    return row -> {
      HarvestJob harvestJob = new HarvestJob();
      harvestJob.id = row.getUUID(HarvestJobTable.id.name());
      harvestJob.name = row.getString(HarvestJobTable.harvestable_name.name());
      harvestJob.harvestableId = row.getInteger(HarvestJobTable.harvestable_id.name());
      harvestJob.type = row.getString(HarvestJobTable.type.name());
      harvestJob.allowErrors = row.getBoolean(HarvestJobTable.allow_errors.name());
      if (row.getValue(HarvestJobTable.record_limit.name()) != null) {
        harvestJob.recordLimit = row.getInteger(HarvestJobTable.record_limit.name());
      }
      if (row.getValue(HarvestJobTable.started.name()) != null) {
        harvestJob.started = row.getLocalDateTime(HarvestJobTable.started.name()).toString();
      }
      if (row.getValue(HarvestJobTable.finished.name()) != null) {
        harvestJob.finished = row.getLocalDateTime(HarvestJobTable.finished.name()).toString();
      }
      if (row.getValue(HarvestJobTable.amount_harvested.name()) != null) {
        harvestJob.amountHarvested = row.getInteger(HarvestJobTable.amount_harvested.name());
      }
      harvestJob.message = row.getString(HarvestJobTable.message.name());
      return harvestJob;
    };
  }

  /**
   * Maps HarvestJob Java Object to JSON.
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


  public UUID id() {
    return id;
  }

  public String type() {
    return type;
  }

  public String name() {
    return name;
  }

}
