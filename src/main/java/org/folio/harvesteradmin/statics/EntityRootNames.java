package org.folio.harvesteradmin.statics;

public class EntityRootNames
{
    // The Harvester's name for the root element of each entity.
    // Needed for wrapping/unwrapping objects when converting to/from the Harvester XML entity schema
    public final static String HARVESTABLE_ROOT_PROPERTY = "harvestable";
    public final static String STORAGE_ROOT_PROPERTY = "storage";
    public final static String TRANSFORMATION_ROOT_PROPERTY = "transformation";
    public final static String STEP_ROOT_PROPERTY = "transformationStep";
    public final static String TRANSFORMATION_STEP_ROOT_PROPERTY = "tsa";

    public final static String HARVESTABLE_SET_ROOT_PROPERTY = "harvestables";
    public final static String STORAGE_SET_ROOT_PROPERTY = "storages";
    public final static String TRANSFORMATION_SET_ROOT_PROPERTY = "transformations";
    public final static String STEP_SET_ROOT_PROPERTY = "transformationSteps";
    public final static String TRANSFORMATION_STEP_SET_ROOT_PROPERTY = "transformationStepAssociations";
}
