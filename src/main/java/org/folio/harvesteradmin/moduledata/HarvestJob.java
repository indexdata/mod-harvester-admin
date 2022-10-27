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
  private String started;
  private String finished;
  private int amountHarvested;
  private String message;

  public HarvestJob(JsonObject json) {
    super(json);
  }

  public HarvestJob() {
    super();
  }

  /**
   * Creates HarvestJob from harvestable JSON.
   */
  public static HarvestJob fromHarvestableJson(JsonObject json) {
    HarvestJob harvestJob = new HarvestJob(json);
    harvestJob.id = harvestJob.uuid;
    harvestJob.harvestableId = Integer.parseInt(json.getString("id"));
    harvestJob.name = json.getString("name");
    harvestJob.type = json.getString("type");
    harvestJob.started = json.getString("lastHarvestStarted");
    harvestJob.finished = json.getString("lastHarvestFinished");
    harvestJob.amountHarvested = Integer.parseInt(json.getString("amountHarvested"));
    harvestJob.message = json.getString("message");
    return harvestJob;
  }

  /**
   * Creates a TupleMapper.
   */
  public static TupleMapper<HarvestJob> tupleMapper() {
    return TupleMapper.mapper(
        harvestJob -> {
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(HarvestJobTable.id.name(), harvestJob.id);
          parameters.put(HarvestJobTable.harvestable_id.name(), harvestJob.harvestableId);
          parameters.put(HarvestJobTable.harvestable_name.name(), harvestJob.name);
          parameters.put(HarvestJobTable.type.name(), harvestJob.type);
          parameters.put(HarvestJobTable.started.name(), harvestJob.started);
          parameters.put(HarvestJobTable.finished.name(), harvestJob.finished);
          parameters.put(HarvestJobTable.amount_harvested.name(), harvestJob.amountHarvested);
          parameters.put(HarvestJobTable.message.name(), harvestJob.message);
          return parameters;
        });
  }

  /**
   * Creates a RowMapper.
   */
  public static RowMapper<HarvestJob> rowMapper() {
    return row -> {
      HarvestJob harvestJob = new HarvestJob();
      harvestJob.id = row.getUUID(HarvestJobTable.id.name());
      harvestJob.name = row.getString(HarvestJobTable.harvestable_name.name());
      harvestJob.harvestableId = row.getInteger(HarvestJobTable.harvestable_id.name());
      harvestJob.type = row.getString(HarvestJobTable.type.name());
      harvestJob.started = row.getLocalDateTime(HarvestJobTable.started.name()).toString();
      harvestJob.finished = row.getLocalDateTime(HarvestJobTable.finished.name()).toString();
      harvestJob.amountHarvested = row.getInteger(HarvestJobTable.amount_harvested.name());
      harvestJob.message = row.getString(HarvestJobTable.message.name());
      return harvestJob;
    };
  }

  /**
   * Gets HarvestJob as JSON.
   */
  public JsonObject asJson() {
    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("name", name);
    json.put("harvestableId", harvestableId);
    json.put("type", type);
    json.put("started", started);
    json.put("finished", finished);
    json.put("amountHarvested", amountHarvested);
    json.put("message", message);
    return json;
  }


  public UUID id() {
    return id;
  }

  public int harvestableId() {
    return harvestableId;
  }

  public String type() {
    return type;
  }

  public String name() {
    return name;
  }

  public String started() {
    return started;
  }

  public String finished() {
    return finished;
  }

  public int amountHarvested() {
    return amountHarvested;
  }

  public String message() {
    return message;
  }
}
