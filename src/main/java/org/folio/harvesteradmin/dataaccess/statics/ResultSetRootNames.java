package org.folio.harvesteradmin.dataaccess.statics;

import java.util.HashMap;
import java.util.Map;

public class ResultSetRootNames {
  public static final Map<String, String> rootOfResultSetByHarvesterPath = new HashMap<>();

  static {
    rootOfResultSetByHarvesterPath.put(ApiPaths.HARVESTER_HARVESTABLES_PATH,
        EntityRootNames.HARVESTABLE_SET_ROOT_PROPERTY);
    rootOfResultSetByHarvesterPath.put(ApiPaths.HARVESTER_STORAGES_PATH,
        EntityRootNames.STORAGE_SET_ROOT_PROPERTY);
    rootOfResultSetByHarvesterPath.put(ApiPaths.HARVESTER_TRANSFORMATIONS_PATH,
        EntityRootNames.TRANSFORMATION_SET_ROOT_PROPERTY);
    rootOfResultSetByHarvesterPath.put(ApiPaths.HARVESTER_STEPS_PATH,
        EntityRootNames.STEP_SET_ROOT_PROPERTY);
    rootOfResultSetByHarvesterPath.put(ApiPaths.HARVESTER_TSAS_PATH,
        EntityRootNames.TRANSFORMATION_STEP_SET_ROOT_PROPERTY);
  }

}
