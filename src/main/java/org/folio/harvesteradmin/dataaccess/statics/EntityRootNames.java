package org.folio.harvesteradmin.dataaccess.statics;

import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_STEPS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_STORAGES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_TRANSFORMATIONS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_TSAS_PATH;

import java.util.HashMap;
import java.util.Map;

public class EntityRootNames {
  // The Harvester's name for the root element of each entity.
  // Needed for wrapping/unwrapping objects when converting to/from the Harvester XML entity schema
  public static final String HARVESTABLE_ROOT_PROPERTY = "harvestable";
  public static final String STORAGE_ROOT_PROPERTY = "storage";
  public static final String TRANSFORMATION_ROOT_PROPERTY = "transformation";
  public static final String STEP_ROOT_PROPERTY = "transformationStep";
  public static final String TRANSFORMATION_STEP_ROOT_PROPERTY = "tsa";

  public static final String HARVESTABLE_SET_ROOT_PROPERTY = "harvestables";
  public static final String STORAGE_SET_ROOT_PROPERTY = "storages";
  public static final String TRANSFORMATION_SET_ROOT_PROPERTY = "transformations";
  public static final String STEP_SET_ROOT_PROPERTY = "transformationSteps";
  public static final String TRANSFORMATION_STEP_SET_ROOT_PROPERTY
      = "transformationStepAssociations";

  private static final Map<String, String> rootOfEntityByHarvesterPath = new HashMap<>();

  static {
    rootOfEntityByHarvesterPath.put(HARVESTER_HARVESTABLES_PATH,
        EntityRootNames.HARVESTABLE_ROOT_PROPERTY);
    rootOfEntityByHarvesterPath.put(HARVESTER_STORAGES_PATH, EntityRootNames.STORAGE_ROOT_PROPERTY);
    rootOfEntityByHarvesterPath.put(HARVESTER_TRANSFORMATIONS_PATH,
        EntityRootNames.TRANSFORMATION_ROOT_PROPERTY);
    rootOfEntityByHarvesterPath.put(HARVESTER_STEPS_PATH, EntityRootNames.STEP_ROOT_PROPERTY);
    rootOfEntityByHarvesterPath.put(HARVESTER_TSAS_PATH,
        EntityRootNames.TRANSFORMATION_STEP_ROOT_PROPERTY);

  }

  /**
   * Get the Harvester's name for the root element of entities from the requested Harvester path.
   * Required for wrapping/unwrapping during data conversions.
   *
   * @param harvesterPath defines the name of root property of an entity
   * @return name of the root element for entities from the Harvester path corresponding to the
   *     given request path
   */
  public static String mapToNameOfRootOfEntity(String harvesterPath) {
    return rootOfEntityByHarvesterPath.get(harvesterPath);
  }

  public static Map<String, String> typeToEmbeddedTypeMap = new HashMap<>();

  static {
    typeToEmbeddedTypeMap.put("CustomTransformStep", "customTransformationStep");
    typeToEmbeddedTypeMap.put("XmlTransformStep", "xmlTransformationStep");
  }

}
