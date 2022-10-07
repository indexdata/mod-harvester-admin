package org.folio.harvesteradmin.test;

import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_STEPS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_STORAGES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_TRANSFORMATIONS_PATH;
import static org.folio.harvesteradmin.test.Api.deleteConfigRecord;
import static org.folio.harvesteradmin.test.Api.getConfigRecord;
import static org.folio.harvesteradmin.test.Api.getConfigRecords;
import static org.folio.harvesteradmin.test.Api.putConfigRecord;
import static org.folio.harvesteradmin.test.Api.putScript;
import static org.folio.harvesteradmin.test.Api.responseJson;
import static org.folio.harvesteradmin.test.Api.postConfigRecord;
import static org.folio.harvesteradmin.test.SampleId.SAMPLES_ID_PREFIX;
import static org.folio.harvesteradmin.test.sampleData.Samples.BASE_STORAGE_ID;
import static org.folio.harvesteradmin.test.sampleData.Samples.BASE_STORAGE_JSON;
import static org.folio.harvesteradmin.test.sampleData.Samples.BASE_TRANSFORMATION_ID;
import static org.folio.harvesteradmin.test.sampleData.Samples.BASE_TRANSFORMATION_JSON;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_SCRIPT;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP_ID;
import static org.junit.Assert.assertTrue;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.harvesteradmin.MainVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("JUnitMalformedDeclaration")
@RunWith( VertxUnitRunner.class )
public class HarvesterAdminTestSuite {
  private final Logger logger = LoggerFactory.getLogger( "HarvesterAdminTestSuite" );

  Vertx vertx;
  private static final int PORT_HARVESTER_ADMIN = 9031;
  public static final Header CONTENT_TYPE_JSON = new Header("Content-Type", "application/json");
  public static final Header CONTENT_TYPE_XML = new Header("Content-Type", "application/xml");
  public static final Header OKAPI_TENANT = new Header ("X-Okapi-Tenant", "diku");


  public HarvesterAdminTestSuite() {}

  @Rule
  public final TestName name = new TestName();

  @Before
  public void setUp(TestContext testContext) {
    logger.debug("setUp " + name.getMethodName());

    vertx = Vertx.vertx();

    // Register the testContext exception handler to catch assertThat
    vertx.exceptionHandler(testContext.exceptionHandler());

    System.setProperty("port", String.valueOf(PORT_HARVESTER_ADMIN));
    vertx.deployVerticle(
        MainVerticle.class.getName(), new DeploymentOptions())
        .onComplete(testContext.asyncAssertSuccess(outcome -> {
          deleteSamplesFromLegacyHarvester();
        }));
  }

  @After
  public void tearDown(TestContext context) {
    deleteSamplesFromLegacyHarvester();
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      async.complete();
    }));
  }

  private void deleteSamplesFromLegacyHarvester() {
    deleteRecordsByIdPrefix(THIS_HARVESTABLES_PATH, "harvestables");
    deleteRecordsByIdPrefix(THIS_STORAGES_PATH, "storages");
    deleteRecordsByIdPrefix(THIS_TRANSFORMATIONS_PATH, "transformations");
    deleteRecordsByIdPrefix(THIS_STEPS_PATH, "transformationSteps");
  }

  private void deleteRecordsByIdPrefix(String path, String recordsArrayProperty) {
    RestAssured.port = PORT_HARVESTER_ADMIN;
    JsonObject samples = responseJson(
        getConfigRecords(path, "id=" + SAMPLES_ID_PREFIX + "*", 200));
    JsonArray sampleRecords = samples.getJsonArray(recordsArrayProperty);
    for (Object o : sampleRecords) {
      String id = ((JsonObject) o).getString("id");
      deleteConfigRecord(path, id, 204);
    }
  }

  @Test
  public void canCreateUpdateAndDeleteStorageConfiguration() {
    RestAssured.port = PORT_HARVESTER_ADMIN;
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    JsonObject record = responseJson(
        getConfigRecord(THIS_STORAGES_PATH, BASE_STORAGE_ID.toString(), 200));
    String recordId = record.getString("id");
    record.put("name", "MODIFIED: " + record.getString("name"));
    putConfigRecord(THIS_STORAGES_PATH, recordId, record, 204);
    JsonObject updatedRecord = responseJson(
        getConfigRecord(THIS_STORAGES_PATH, BASE_STORAGE_ID.toString(), 200));
    assertTrue("Name is modified",
        updatedRecord.getString("name").startsWith("MODIFIED"));
    deleteConfigRecord(THIS_STORAGES_PATH, recordId, 204);
  }

  @Test
  public void canCreateUpdateAndDeleteTransformationPipelineNoSteps() {
    RestAssured.port = PORT_HARVESTER_ADMIN;
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    JsonObject record = responseJson(
        getConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 200));
    String recordId = record.getString("id");
    record.put("name", "MODIFIED: " + record.getString("name"));
    putConfigRecord(THIS_TRANSFORMATIONS_PATH, recordId, record, 204);
    JsonObject updatedRecord = responseJson(
        getConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 200));
    assertTrue("Name is modified",
        updatedRecord.getString("name").startsWith("MODIFIED"));
    deleteConfigRecord(THIS_TRANSFORMATIONS_PATH, recordId, 204);
  }

  @Test
  public void canCreateHarvestable(TestContext testContext )
  {
    SampleId harvestableId = new SampleId(1);
    JsonObject harvestable =
        new JsonObject(
            "{\n"
                + "  \"id\": \"" + harvestableId.fullId() +"\",\n"
                + "  \"name\": \"Test harvest job\",\n"
                + "  \"type\": \"oaiPmh\",\n"
                + "  \"enabled\": \"false\",\n"
                + "  \"harvestImmediately\": \"false\",\n"
                + "  \"storage\": {\n"
                + "    \"entityType\": \"inventoryStorageEntity\",\n"
                + "    \"id\": \"" + BASE_STORAGE_ID.fullId() + "\"\n"
                + "  },\n"
                + "  \"transformation\": {\n"
                + "    \"entityType\": \"basicTransformation\",\n"
                + "    \"id\": \"" + BASE_TRANSFORMATION_ID.fullId() + "\"\n"
                + "  },\n"
                + "  \"metadataPrefix\": \"marc21\",\n"
                + "  \"oaiSetName\": \"PALCI_RESHARE\",\n"
                + "  \"url\": \"https://na01.alma.exlibrisgroup"
                + ".com/view/oai/01SSHELCO_BLMSBRG/request\",\n"
                + "  \"dateFormat\": \"yyyy-MM-dd'T'hh:mm:ss'Z'\"\n"
            + "}"
        );
    RestAssured.port = PORT_HARVESTER_ADMIN;
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    JsonObject result = responseJson(
        postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 201));
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 200);
  }

  @Test
  public void cannotCreateHarvestableWithWrongStorageId()
  {
    final SampleId harvestableId = new SampleId(1);
    JsonObject harvestable =
        new JsonObject(
            "{\n"
                + "  \"id\": \"" + harvestableId +"\",\n"
                + "  \"name\": \"Test harvest job\",\n"
                + "  \"type\": \"oaiPmh\",\n"
                + "  \"enabled\": \"false\",\n"
                + "  \"harvestImmediately\": \"false\",\n"
                + "  \"storage\": {\n"
                + "    \"entityType\": \"inventoryStorageEntity\",\n"
                + "    \"id\": \"" + BASE_STORAGE_ID + "\"\n"
                + "  },\n"
                + "  \"transformation\": {\n"
                + "    \"entityType\": \"basicTransformation\",\n"
                + "    \"id\": \"" + BASE_TRANSFORMATION_ID + "\"\n"
                + "  },\n"
                + "  \"metadataPrefix\": \"marc21\",\n"
                + "  \"oaiSetName\": \"PALCI_RESHARE\",\n"
                + "  \"url\": \"https://na01.alma.exlibrisgroup"
                + ".com/view/oai/01SSHELCO_BLMSBRG/request\",\n"
                + "  \"dateFormat\": \"yyyy-MM-dd'T'hh:mm:ss'Z'\"\n"
                + "}"
        );
    RestAssured.port = PORT_HARVESTER_ADMIN;
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 500);
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 404);
  }

  @Test
  public void cannotCreateHarvestableWithWrongTransformationId()
  {
    final SampleId harvestableId = new SampleId(1);
    JsonObject harvestable =
        new JsonObject(
            "{\n"
                + "  \"id\": \"" + harvestableId +"\",\n"
                + "  \"name\": \"Test harvest job\",\n"
                + "  \"type\": \"oaiPmh\",\n"
                + "  \"enabled\": \"false\",\n"
                + "  \"harvestImmediately\": \"false\",\n"
                + "  \"storage\": {\n"
                + "    \"entityType\": \"inventoryStorageEntity\",\n"
                + "    \"id\": \"" + BASE_STORAGE_ID + "\"\n"
                + "  },\n"
                + "  \"transformation\": {\n"
                + "    \"entityType\": \"basicTransformation\",\n"
                + "    \"id\": \"" + BASE_TRANSFORMATION_ID + "\"\n"
                + "  },\n"
                + "  \"metadataPrefix\": \"marc21\",\n"
                + "  \"oaiSetName\": \"PALCI_RESHARE\",\n"
                + "  \"url\": \"https://na01.alma.exlibrisgroup"
                + ".com/view/oai/01SSHELCO_BLMSBRG/request\",\n"
                + "  \"dateFormat\": \"yyyy-MM-dd'T'hh:mm:ss'Z'\"\n"
                + "}"
        );
    RestAssured.port = PORT_HARVESTER_ADMIN;
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 500);
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 404);
  }

  @Test
  public void canCreateUpdateAndDeleteStep()
  {
    RestAssured.port = PORT_HARVESTER_ADMIN;
    postConfigRecord(SAMPLE_STEP, THIS_STEPS_PATH, 201);
    JsonObject record = responseJson(
        getConfigRecord(THIS_STEPS_PATH, SAMPLE_STEP_ID.toString(), 200));
    record.put("name", "MODIFIED: " + record.getString("name"));
    putConfigRecord(THIS_STEPS_PATH, SAMPLE_STEP_ID.toString(), record, 204);
    JsonObject updatedRecord = responseJson(
        getConfigRecord(THIS_STEPS_PATH, SAMPLE_STEP_ID.toString(), 200));
    assertTrue("Name is modified",
        updatedRecord.getString("name").startsWith("MODIFIED"));
    deleteConfigRecord(THIS_STEPS_PATH, SAMPLE_STEP_ID.toString(), 204);

  }

  @Test
  public void canPopulateScriptToStep()
  {
    RestAssured.port = PORT_HARVESTER_ADMIN;
    postConfigRecord(SAMPLE_STEP, THIS_STEPS_PATH, 201);
    putScript(SAMPLE_STEP.getString("id"), SAMPLE_STEP.getString("name"), SAMPLE_SCRIPT,
        204);
  }
}
