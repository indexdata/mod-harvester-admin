package org.folio.harvesteradmin.dataaccess.statics;

import java.util.HashMap;
import java.util.Map;

public class ApiPaths {

  // API path to the underlying Harvester back-end
  public static final String HARVESTER_BASE_PATH = "/harvester/records";


  public static final String HARVESTABLES_PATH = "/harvestables";
  public static final String XML_BULK_PATH = "/xml-bulk";
  public static final String OAI_PMH_PATH = "/oai-pmh";
  public static final String STORAGES_PATH = "/storages";
  public static final String TRANSFORMATIONS_PATH = "/transformations";
  public static final String STEPS_PATH = "/steps";
  public static final String TRANSFORMATIONS_STEPS_PATH = "/tsas";

  // Legacy Harvester APIs
  public static final String HARVESTER_HARVESTABLES_PATH = HARVESTER_BASE_PATH + HARVESTABLES_PATH;
  public static final String HARVESTER_STORAGES_PATH = HARVESTER_BASE_PATH + STORAGES_PATH;
  public static final String HARVESTER_TRANSFORMATIONS_PATH =
      HARVESTER_BASE_PATH + TRANSFORMATIONS_PATH;
  public static final String HARVESTER_STEPS_PATH = HARVESTER_BASE_PATH + STEPS_PATH;
  public static final String HARVESTER_TSAS_PATH =
      HARVESTER_BASE_PATH + TRANSFORMATIONS_STEPS_PATH;

  // FOLIO harvester-admin APIs
  // API path to this FOLIO module
  public static final String THIS_API_BASE_PATH = "/harvester-admin";
  public static final String THIS_HARVESTABLES_PATH = THIS_API_BASE_PATH + HARVESTABLES_PATH;
  public static final String THIS_HARVESTABLES_XML_BULK_PATH =
      THIS_API_BASE_PATH + HARVESTABLES_PATH + XML_BULK_PATH;
  public static final String THIS_HARVESTABLES_OAI_PMH_PATH =
      THIS_API_BASE_PATH + HARVESTABLES_PATH + OAI_PMH_PATH;
  public static final String THIS_STORAGES_PATH = THIS_API_BASE_PATH + STORAGES_PATH;
  public static final String THIS_TRANSFORMATIONS_PATH = THIS_API_BASE_PATH + TRANSFORMATIONS_PATH;
  public static final String THIS_STEPS_PATH = THIS_API_BASE_PATH + STEPS_PATH;
  public static final String THIS_TRANSFORMATIONS_STEPS_PATH =
      THIS_API_BASE_PATH + TRANSFORMATIONS_STEPS_PATH;
  // Run jobs
  // XSLT scripts

  public static final Map<String, String> harvesterPathByRequestPath = new HashMap<>();

  static {
    harvesterPathByRequestPath.put(THIS_HARVESTABLES_PATH, HARVESTER_HARVESTABLES_PATH);
    harvesterPathByRequestPath.put(THIS_HARVESTABLES_XML_BULK_PATH, HARVESTER_HARVESTABLES_PATH);
    harvesterPathByRequestPath.put(THIS_HARVESTABLES_OAI_PMH_PATH, HARVESTER_HARVESTABLES_PATH);
    harvesterPathByRequestPath.put(THIS_STORAGES_PATH, HARVESTER_STORAGES_PATH);
    harvesterPathByRequestPath.put(THIS_TRANSFORMATIONS_PATH, HARVESTER_TRANSFORMATIONS_PATH);
    harvesterPathByRequestPath.put(THIS_STEPS_PATH, HARVESTER_STEPS_PATH);
    harvesterPathByRequestPath.put(THIS_TRANSFORMATIONS_STEPS_PATH, HARVESTER_TSAS_PATH);
  }

}
