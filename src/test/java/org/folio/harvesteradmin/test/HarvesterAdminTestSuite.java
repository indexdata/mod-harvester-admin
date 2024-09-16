package org.folio.harvesteradmin.test;

import static io.restassured.RestAssured.given;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.THIS_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.THIS_STEPS_PATH;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.THIS_STORAGES_PATH;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.THIS_TRANSFORMATIONS_PATH;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.THIS_TRANSFORMATIONS_STEPS_PATH;
import static org.folio.harvesteradmin.test.Api.*;
import static org.folio.harvesteradmin.test.SampleId.SAMPLES_ID_PREFIX;
import static org.folio.harvesteradmin.test.sampleData.Samples.BASE_STORAGE_ID;
import static org.folio.harvesteradmin.test.sampleData.Samples.BASE_STORAGE_JSON;
import static org.folio.harvesteradmin.test.sampleData.Samples.BASE_TRANSFORMATION_ID;
import static org.folio.harvesteradmin.test.sampleData.Samples.BASE_TRANSFORMATION_JSON;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_SCRIPT;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP_2;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP_2_ID;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.harvesteradmin.MainVerticle;
import org.folio.harvesteradmin.test.fakestorage.FakeFolioApis;
import org.folio.harvesteradmin.test.utils.NetworkUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.tlib.postgres.testing.TenantPgPoolContainer;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RunWith( VertxUnitRunner.class )
public class HarvesterAdminTestSuite {
  private static final Logger logger = LoggerFactory.getLogger( "HarvesterAdminTestSuite" );

  static final String TENANT = "mha_test";
  static Vertx vertx;
  public static int PORT_HARVESTER_ADMIN = NetworkUtils.nextFreePort();
  public static final int PORT_OKAPI = 9031;
  public static final Header CONTENT_TYPE_JSON = new Header("Content-Type", "application/json");
  public static final Header CONTENT_TYPE_XML = new Header("Content-Type", "application/xml");
  public static final Header OKAPI_TENANT = new Header (XOkapiHeaders.TENANT, TENANT);
  public static final Header OKAPI_URL = new Header (XOkapiHeaders.URL, "http://localhost:" + PORT_OKAPI);
  public static final Header OKAPI_TOKEN = new Header(XOkapiHeaders.TOKEN,"eyJhbGciOiJIUzUxMiJ9eyJzdWIiOiJhZG1pbiIsInVzZXJfaWQiOiI3OWZmMmE4Yi1kOWMzLTViMzktYWQ0YS0wYTg0MDI1YWIwODUiLCJ0ZW5hbnQiOiJ0ZXN0X3RlbmFudCJ9BShwfHcNClt5ZXJ8ImQTMQtAM1sQEnhsfWNmXGsYVDpuaDN3RVQ9");


  public HarvesterAdminTestSuite() {}

  @ClassRule
  public static PostgreSQLContainer<?> postgresSQLContainer = TenantPgPoolContainer.create();


  @Rule
  public final TestName name = new TestName();

  @BeforeClass
  public static void setUp(TestContext testContext) {

    vertx = Vertx.vertx();

    // Register the testContext exception handler to catch assertThat
    vertx.exceptionHandler(testContext.exceptionHandler());
    RestAssured.port = PORT_HARVESTER_ADMIN;

    System.setProperty("port", String.valueOf(PORT_HARVESTER_ADMIN));
    vertx.deployVerticle(
        MainVerticle.class.getName(), new DeploymentOptions())
        .onComplete(testContext.asyncAssertSuccess(outcome -> new FakeFolioApis(vertx, testContext)));

  }

  @Before @After
  public void cleanUpSampleData() {
    deleteSamplesFromLegacyHarvester();
  }

  @AfterClass
  public static void tearDown(TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> async.complete()));
  }

  private void deleteSamplesFromLegacyHarvester() {
    deleteRecordsByIdPrefix(THIS_HARVESTABLES_PATH, "harvestables");
    deleteRecordsByIdPrefix(THIS_STORAGES_PATH, "storages");
    deleteTsasByStepIdPrefix();
    deleteRecordsByIdPrefix(THIS_TRANSFORMATIONS_PATH, "transformations");
    deleteRecordsByIdPrefix(THIS_STEPS_PATH, "transformationSteps");
  }

  void initModuleDatabase () {
    tenantOp(TENANT, new JsonObject().put("module_to", "mod-harvester-admin-0.0.0"), null);
  }

  void tenantOp(String tenant, JsonObject tenantAttributes, String expectedError) {
    ExtractableResponse<Response> response = RestAssured.given()
            .header(XOkapiHeaders.TENANT, tenant)
            .contentType(ContentType.JSON)
            .body(tenantAttributes.encode())
            .post("/_/tenant")
            .then()
            .extract();

    if (response.statusCode() == 204) {
      return;
    }
    assertThat(response.statusCode(), is(201));
    String location = response.header("Location");
    JsonObject tenantJob = new JsonObject(response.asString());
    assertThat(location, is("/_/tenant/" + tenantJob.getString("id")));

    RestAssured.given()
            .header(XOkapiHeaders.TENANT, tenant)
            .get(location + "?wait=10000")
            .then().statusCode(200)
            .body("complete", is(true))
            .body("error", is(expectedError));

    RestAssured.given()
            .header(XOkapiHeaders.TENANT, tenant)
            .delete(location)
            .then().statusCode(204);
  }


  private void deleteRecordsByIdPrefix(String path, String recordsArrayProperty) {
    JsonObject samples = responseJson(
        getConfigRecords(path, "id=" + SAMPLES_ID_PREFIX + "*", 200));
    JsonArray sampleRecords = samples.getJsonArray(recordsArrayProperty);
    for (Object o : sampleRecords) {
      String id = ((JsonObject) o).getString("id");
      deleteConfigRecord(path, id, 204);
    }
  }

  private void deleteTsasByStepIdPrefix() {
    Response response =
        getConfigRecords(THIS_TRANSFORMATIONS_STEPS_PATH, "step.id=" +SAMPLES_ID_PREFIX + "*",200);
    JsonObject transformationStepAssociations = new JsonObject(response.body().asString());
    if (!transformationStepAssociations.getJsonArray("transformationStepAssociations").isEmpty()) {
      logger.info("TSAs to delete: ");
    }
    for (Object o : transformationStepAssociations.getJsonArray("transformationStepAssociations")) {
      logger.info(((JsonObject) o).encodePrettily());
    }
    for (Object o : transformationStepAssociations.getJsonArray("transformationStepAssociations")) {
      JsonObject tsas = (JsonObject) o;
      logger.info("Found tsas " + tsas.encodePrettily());
      String tsaId = tsas.getString("id");
      logger.info("Trying to delete TSA " + tsaId);
      deleteConfigRecord(THIS_TRANSFORMATIONS_STEPS_PATH, tsas.getString("id"),204);
    }
  }

  @Test
  public void canCreateUpdateAndDeleteStorageConfiguration() {
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
  public void attemptAtUpdatingNonExistingRecordWillReturn404() {
    putConfigRecord(THIS_STORAGES_PATH, "99887766554433", BASE_STORAGE_JSON, 404);
  }

  @Test
  public void canCreateUpdateAndDeleteTransformationPipelineNoSteps() {
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
  public void canCreateHarvestableWithReferencesById() {
    SampleId harvestableId = new SampleId(1);
    JsonObject harvestable =
        new JsonObject(
            "{\n"
                + "  \"id\": \"" + harvestableId.fullId() +"\",\n"
                + "  \"name\": \"Test harvest job (modhaadm unit tests)\",\n"
                + "  \"type\": \"oaiPmh\",\n"
                + "  \"enabled\": \"false\",\n"
                + "  \"harvestImmediately\": \"false\",\n"
                + "  \"lastUpdated\": \"2022-12-07T15:20:49.507Z\",\n"
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
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 201);
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 200);
  }

  @Test
  public void canCreateHarvestableWithReferencesByName() {
    SampleId harvestableId = new SampleId(1);
    JsonObject harvestable =
        new JsonObject(
            "{\n"
                + "  \"id\": \"" + harvestableId.fullId() +"\",\n"
                + "  \"name\": \"Test harvest job (modhaadm unit tests)\",\n"
                + "  \"type\": \"oaiPmh\",\n"
                + "  \"enabled\": \"false\",\n"
                + "  \"harvestImmediately\": \"false\",\n"
                + "  \"lastUpdated\": \"2022-12-07T15:20:49.507Z\",\n"
                + "  \"storage\": {\n"
                + "    \"name\": \"" + BASE_STORAGE_JSON.getString("name") + "\"\n"
                + "  },\n"
                + "  \"transformation\": {\n"
                + "    \"name\": \"" + BASE_TRANSFORMATION_JSON.getString("name") + "\"\n"
                + "  },\n"
                + "  \"metadataPrefix\": \"marc21\",\n"
                + "  \"oaiSetName\": \"PALCI_RESHARE\",\n"
                + "  \"url\": \"https://na01.alma.exlibrisgroup"
                + ".com/view/oai/01SSHELCO_BLMSBRG/request\",\n"
                + "  \"dateFormat\": \"yyyy-MM-dd'T'hh:mm:ss'Z'\"\n"
                + "}"
        );
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 201);
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 200);
  }

  @Test
  public void cannotCreateOaiPmhHarvestableWithoutOaiSetName() {
    SampleId harvestableId = new SampleId(1);
    JsonObject harvestable =
        new JsonObject(
            "{\n"
                + "  \"id\": \"" + harvestableId.fullId() +"\",\n"
                + "  \"name\": \"Test harvest job (modhaadm unit tests)\",\n"
                + "  \"type\": \"oaiPmh\",\n"
                + "  \"enabled\": \"false\",\n"
                + "  \"harvestImmediately\": \"false\",\n"
                + "  \"lastUpdated\": \"2022-12-07T15:20:49.507Z\",\n"
                + "  \"storage\": {\n"
                + "    \"entityType\": \"inventoryStorageEntity\",\n"
                + "    \"id\": \"" + BASE_STORAGE_ID.fullId() + "\"\n"
                + "  },\n"
                + "  \"transformation\": {\n"
                + "    \"entityType\": \"basicTransformation\",\n"
                + "    \"id\": \"" + BASE_TRANSFORMATION_ID.fullId() + "\"\n"
                + "  },\n"
                + "  \"metadataPrefix\": \"marc21\",\n"
                + "  \"url\": \"https://na01.alma.exlibrisgroup"
                + ".com/view/oai/01SSHELCO_BLMSBRG/request\",\n"
                + "  \"dateFormat\": \"yyyy-MM-dd'T'hh:mm:ss'Z'\"\n"
                + "}"
        );
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 400);
  }

  @Test
  public void canCreateHarvestableButCannotGetJobLogIfNotRun()
  {
    SampleId harvestableId = new SampleId(1);
    JsonObject harvestable =
        new JsonObject(
            "{\n"
                + "  \"id\": \"" + harvestableId.fullId() +"\",\n"
                + "  \"name\": \"Test harvest job (modhaadm unit tests)\",\n"
                + "  \"type\": \"oaiPmh\",\n"
                + "  \"enabled\": \"false\",\n"
                + "  \"harvestImmediately\": \"false\",\n"
                + "  \"lastUpdated\": \"2022-12-07T15:20:49.507Z\",\n"
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
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 201);
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 200);
    getJobLog(harvestableId.toString(), 404);
  }

  @Test
  public void cannotCreateTwoHarvestablesWithSameId () {
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
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 422);

  }

  @Test
  public void cannotGetLogsForNonExistingHarvestable() {
    getJobLog("9865320", 404);
  }

  //@Test
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
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 422);
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
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 422);
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 404);
  }

  @Test
  public void badQueryWillReturn400() {
    getConfigRecords(THIS_HARVESTABLES_PATH, "badFieldName=x", 400);
  }

  @Test
  public void canCreateUpdateAndDeleteStep()
  {
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
    postConfigRecord(SAMPLE_STEP, THIS_STEPS_PATH, 201);
    putScript(SAMPLE_STEP.getString("id"), SAMPLE_STEP.getString("name"), SAMPLE_SCRIPT,
        204);
    getScript(SAMPLE_STEP.getString("id"), 200);
  }

  @Test
  public void canCreateAndDeleteTransformationWithSteps() {
    postConfigRecord(SAMPLE_STEP, THIS_STEPS_PATH, 201);
    postConfigRecord(SAMPLE_STEP_2, THIS_STEPS_PATH, 201);
    JsonObject pipeline = new JsonObject(BASE_TRANSFORMATION_JSON.encode());
    JsonArray stepAssociations = new JsonArray();
    stepAssociations.add(new JsonObject().put("stepId", SAMPLE_STEP.getString("id")));
    stepAssociations.add(new JsonObject().put("stepId", SAMPLE_STEP_2.getString("id")));
    pipeline.put("stepAssociations", stepAssociations);
    postConfigRecord(pipeline, THIS_TRANSFORMATIONS_PATH, 201);
    getConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 200);
    deleteConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 204);
    getConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 404);
  }

  @Test
  public void canAssociateAStepWithATransformation() {
    postConfigRecord(SAMPLE_STEP, THIS_STEPS_PATH, 201);
    postConfigRecord(SAMPLE_STEP_2, THIS_STEPS_PATH, 201);
    JsonObject pipeline = new JsonObject(BASE_TRANSFORMATION_JSON.encode());
    JsonArray stepAssociations = new JsonArray();
    stepAssociations.add(new JsonObject().put("stepId", SAMPLE_STEP.getString("id")));
    pipeline.put("stepAssociations", stepAssociations);
    postConfigRecord(pipeline, THIS_TRANSFORMATIONS_PATH, 201);
    getConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 200);
    JsonObject tsa = new JsonObject(
        "{\n"
            + "  \"step\": { \n"
            + "    \"id\": \"" + SAMPLE_STEP_2_ID + "\"\n"
            + "  },\n"
            + "  \"transformation\": \"" + BASE_TRANSFORMATION_ID +"\",\n"
            + "  \"position\": \"2\"\n"
            + "}"
    );
    postConfigRecord(tsa, THIS_TRANSFORMATIONS_STEPS_PATH, 201);
    getConfigRecord(THIS_TRANSFORMATIONS_PATH,BASE_TRANSFORMATION_ID.toString());
  }

  @Test
  public void canAssociateAStepWithATransformationByNames() {
    postConfigRecord(SAMPLE_STEP, THIS_STEPS_PATH, 201);
    postConfigRecord(SAMPLE_STEP_2, THIS_STEPS_PATH, 201);
    JsonObject pipeline = new JsonObject(BASE_TRANSFORMATION_JSON.encode());
    JsonArray stepAssociations = new JsonArray();
    stepAssociations.add(new JsonObject().put("stepId", SAMPLE_STEP.getString("id")));
    pipeline.put("stepAssociations", stepAssociations);
    postConfigRecord(pipeline, THIS_TRANSFORMATIONS_PATH, 201);
    Response firstResponse =
        getConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 200);
    logger.info("First response: " + firstResponse.body().asPrettyString());
    JsonObject tsa = new JsonObject(
        "{\n"
            + "  \"step\": { \n"
            + "    \"name\": \"" + SAMPLE_STEP_2.getString("name") + "\"\n"
            + "  },\n"
            + "  \"transformationName\": \"" + BASE_TRANSFORMATION_JSON.getString("name") +"\",\n"
            + "  \"position\": \"2\"\n"
            + "}"
    );
    logger.info("Posting tsa " + tsa.encodePrettily());
    postConfigRecord(tsa, THIS_TRANSFORMATIONS_STEPS_PATH, 201);
    getConfigRecord(THIS_TRANSFORMATIONS_PATH,BASE_TRANSFORMATION_ID.toString());
  }

  @Test
  public void canCreateTransformationAssociatedWithAStepByStepName() {
    postConfigRecord(SAMPLE_STEP, THIS_STEPS_PATH, 201);
    postConfigRecord(SAMPLE_STEP_2, THIS_STEPS_PATH, 201);
    JsonObject pipeline = new JsonObject(BASE_TRANSFORMATION_JSON.encode());
    JsonArray stepAssociations = new JsonArray();
    stepAssociations.add(new JsonObject().put("stepName", SAMPLE_STEP.getString("name")));
    pipeline.put("stepAssociations", stepAssociations);
    postConfigRecord(pipeline, THIS_TRANSFORMATIONS_PATH, 201);
    getConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 200);
    JsonObject tsa = new JsonObject(
        "{\n"
            + "  \"step\": { \n"
            + "    \"id\": \"" + SAMPLE_STEP_2_ID + "\"\n"
            + "  },\n"
            + "  \"transformation\": \"" + BASE_TRANSFORMATION_ID +"\",\n"
            + "  \"position\": \"2\"\n"
            + "}"
    );
    postConfigRecord(tsa, THIS_TRANSFORMATIONS_STEPS_PATH, 201);
  }

  @Test
  public void cannotCreateTransformationWithNonExistingStep () {
    postConfigRecord(SAMPLE_STEP, THIS_STEPS_PATH, 201);
    postConfigRecord(SAMPLE_STEP_2, THIS_STEPS_PATH, 201);
    JsonObject pipeline = new JsonObject(BASE_TRANSFORMATION_JSON.encode());
    JsonArray stepAssociations = new JsonArray();
    stepAssociations.add(new JsonObject().put("stepId", SAMPLE_STEP.getString("id")));
    stepAssociations.add(new JsonObject().put("stepId", SAMPLE_STEP_2.getString("id")));
    stepAssociations.add(new JsonObject().put("stepId", "bad-step"));
    pipeline.put("stepAssociations", stepAssociations);
    postConfigRecord(pipeline, THIS_TRANSFORMATIONS_PATH, 422);
  }

  // @Test - disabled because at this point you can
  public void cannotDeleteTransformationThatIsInUse() {
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
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 201);
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 200);
    deleteConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 204);
    getConfigRecord(THIS_TRANSFORMATIONS_PATH, BASE_TRANSFORMATION_ID.toString(), 200);
  }

  @Test
  public void cannotDeleteStorageThatIsInUse() {
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
    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 201);
    getConfigRecord(THIS_HARVESTABLES_PATH, harvestableId.toString(), 200);
    deleteConfigRecord(THIS_STORAGES_PATH, BASE_STORAGE_ID.toString(), 400);
    getConfigRecord(THIS_STORAGES_PATH, BASE_STORAGE_ID.toString(), 200);
  }

  @Test
  public void cannotPostConfigWithAlreadyExistingName() {
    JsonObject storageWithNoId = BASE_STORAGE_JSON.copy();
    storageWithNoId.remove("id");
    JsonObject transformationWithNoId = BASE_TRANSFORMATION_JSON.copy();
    transformationWithNoId.remove("id");

    postConfigRecord(BASE_STORAGE_JSON, THIS_STORAGES_PATH, 201);
    Response resp1 = postConfigRecord(storageWithNoId, THIS_STORAGES_PATH, 422);
    assertTrue(resp1.body().asPrettyString().contains("name 'BASE_STORAGE' exists already"));
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    Response resp2 = postConfigRecord(transformationWithNoId, THIS_TRANSFORMATIONS_PATH, 422);
    assertTrue(resp2.body().asPrettyString().contains("name 'BASE_TRANSFORMATION' exists already"));
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
        postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 201);
        harvestable.remove("id");
        Response resp3 = postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 422);
        assertTrue(resp3.body().asPrettyString().contains("name 'Test harvest job' exists already"));

  }

  @Test
  public void canGetPreviousJob () {
    initModuleDatabase();
    Response response = given().port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
            .get("harvester-admin/previous-jobs")
        .then()
        .log().ifValidationFails().statusCode(200).extract().response();
    logger.info("canGetPreviousJobs response: " + response.asPrettyString());
  }

  @Test
  public void willPurgeAgedJobLogsUsingDefaultThreshold () {
    initModuleDatabase();

    Response response = given().port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
            .get("harvester-admin/previous-jobs")
            .then()
            .log().ifValidationFails().statusCode(200).extract().response();
    logger.info("will purge jobs response: " + response.asPrettyString());

    given().port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
            .get("harvester-admin/previous-jobs")
            .then()
            .log().ifValidationFails().statusCode(200).extract().response();

    LocalDateTime now = LocalDateTime.now();
    final LocalDateTime agedJobStartedTime = now.minusMonths(3).minusDays(1).truncatedTo(ChronoUnit.SECONDS);
    final LocalDateTime agedJobFinishedTime = agedJobStartedTime.plusMinutes(2);
    final LocalDateTime newerJobStartedTime = now.minusMonths(2).truncatedTo(ChronoUnit.SECONDS);
    final LocalDateTime newerJobFinishedTime = newerJobStartedTime.plusMinutes(3);

    JsonObject transformation = BASE_TRANSFORMATION_JSON;
    String transformationId = transformation.getString("id");
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);

    JsonObject agedJobJson =
            new JsonObject(
                    "    {\n" +
                    "      \"id\" : \"" + UUID.randomUUID() + "\",\n" +
                    "      \"name\" : \"fake job log\",\n" +
                    "      \"harvestableId\" : 672813240090200,\n" +
                    "      \"type\" : \"xmlBulk\",\n" +
                    "      \"url\" : \"http://fileserver/xml/\",\n" +
                    "      \"allowErrors\" : true,\n" +
                    "      \"transformation\" : \""+ transformationId +"\",\n" +
                    "      \"storage\" : \"Batch Upsert Inventory\",\n" +
                    "      \"status\" : \"OK\",\n" +
                    "      \"started\" : \""+agedJobStartedTime+"\",\n" +
                    "      \"finished\" : \""+agedJobFinishedTime+"\",\n" +
                    "      \"amountHarvested\" : 5,\n" +
                    "      \"message\" : \"  Instances_processed/loaded/deletions(signals)/failed:__5___5___0(0)___0_ Holdings_records_processed/loaded/deleted/failed:__13___13___0___0_ Items_processed/loaded/deleted/failed:__4___4___0___0_ Source_records_processed/loaded/deleted/failed:__0___0___0___0_\"\n" +
                    "    }\n");

    JsonObject newerJobJson =
            new JsonObject(
                    "    {\n" +
                            "      \"id\" : \"" + UUID.randomUUID() + "\",\n" +
                            "      \"name\" : \"fake job log\",\n" +
                            "      \"harvestableId\" : 672813240090200,\n" +
                            "      \"type\" : \"xmlBulk\",\n" +
                            "      \"url\" : \"http://fileserver/xml/\",\n" +
                            "      \"allowErrors\" : true,\n" +
                            "      \"transformation\" : \"" + transformationId + "\",\n" +
                            "      \"storage\" : \"Batch Upsert Inventory\",\n" +
                            "      \"status\" : \"OK\",\n" +
                            "      \"started\" : \""+newerJobStartedTime+"\",\n" +
                            "      \"finished\" : \""+newerJobFinishedTime+"\",\n" +
                            "      \"amountHarvested\" : 3,\n" +
                            "      \"message\" : \"  Instances_processed/loaded/deletions(signals)/failed:__3___3___0(0)___0_ Holdings_records_processed/loaded/deleted/failed:__8___8___0___0_ Items_processed/loaded/deleted/failed:__2___2___0___0_ Source_records_processed/loaded/deleted/failed:__0___0___0___0_\"\n" +
                            "    }\n");

    given().port(PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
            .body(agedJobJson.encode())
            .contentType(ContentType.JSON)
            .post("harvester-admin/previous-jobs")
            .then()
            .log().ifValidationFails().statusCode(201).extract().response();

    given().port(PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
            .body(newerJobJson.encode())
            .contentType(ContentType.JSON)
            .post("harvester-admin/previous-jobs")
            .then()
            .log().ifValidationFails().statusCode(201).extract().response();

    RestAssured
            .given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
            .contentType(ContentType.JSON)
            .get("harvester-admin/previous-jobs")
            .then().statusCode(200)
            .body("totalRecords",  is(2));

    final RequestSpecification timeoutConfig = timeoutConfig(10000);

    given()
            .port(PORT_OKAPI)
            .header(OKAPI_TENANT)
            .header(OKAPI_URL)
            .header(OKAPI_TOKEN)
            .contentType(ContentType.JSON)
            .header(XOkapiHeaders.REQUEST_ID, "purge-aged-logs")
            .spec(timeoutConfig)
            .when().post("/harvester-admin/purge-aged-logs")
            .then().log().ifValidationFails().statusCode(204)
            .extract().response();

    RestAssured
            .given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
            .contentType(ContentType.JSON)
            .get("harvester-admin/previous-jobs")
            .then().statusCode(200)
            .body("totalRecords",  is(1));

  }

  @Test
  public void willPurgeAgedJobLogsUsingConfiguredThreshold () {

  }

  public static RequestSpecification timeoutConfig(int timeOutInMilliseconds) {
    return new RequestSpecBuilder()
            .setConfig(RestAssured.config()
                    .httpClient(HttpClientConfig.httpClientConfig()
                            .setParam("http.connection.timeout", timeOutInMilliseconds)
                            .setParam("http.socket.timeout", timeOutInMilliseconds)))
            .build();
  }

  @Test
  public void deletingNonExistingConfigWillReturnNotFound() {
    deleteConfigRecord(THIS_TRANSFORMATIONS_PATH, "998877665544", 404);
    deleteConfigRecord(THIS_HARVESTABLES_PATH, "998877665544", 404);
    deleteConfigRecord(THIS_STEPS_PATH, "998877665544", 404);
  }

  @Test
  public void getByIdWithInvalidCharactersReturn400() {
    getConfigRecord(THIS_HARVESTABLES_PATH, "æøå", 400);
  }

  @Test
  public void putByIdWithInvalidCharactersReturn400() {
    putConfigRecord(THIS_STORAGES_PATH, "æøå", BASE_STORAGE_JSON, 400);
  }

}
