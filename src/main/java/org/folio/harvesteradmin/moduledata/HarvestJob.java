package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import org.folio.harvesteradmin.modulestorage.HarvestAdminStorage;


public class HarvestJob extends Entity {

  public HarvestJob(JsonObject json) {
    super(json);
  }

  /**
   * Creates a TupleMapper.
   */
  public static TupleMapper<HarvestJob> tupleMapper() {
    return TupleMapper.mapper(
        harvestJob -> {
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(HarvestAdminStorage.HarvestJobTable.id.name(), harvestJob.getId());
          parameters.put(HarvestAdminStorage.HarvestJobTable.harvestable_id.name(),
              Integer.parseInt(harvestJob.json.getString("id")));
          parameters.put(HarvestAdminStorage.HarvestJobTable.harvestable_name.name(),
              harvestJob.json.getString("name"));
          parameters.put(HarvestAdminStorage.HarvestJobTable.started.name(),
              harvestJob.json.getString("lastHarvestStarted"));
          parameters.put(HarvestAdminStorage.HarvestJobTable.finished.name(),
              harvestJob.json.getString("lastHarvestFinished"));
          return parameters;
        });
  }
}
