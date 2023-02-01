package org.folio.harvesteradmin.test;

import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_STEPS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_STORAGES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_TRANSFORMATIONS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_TRANSFORMATIONS_STEPS_PATH;
import static org.folio.harvesteradmin.test.Api.deleteConfigRecord;
import static org.folio.harvesteradmin.test.Api.getConfigRecord;
import static org.folio.harvesteradmin.test.Api.getConfigRecords;
import static org.folio.harvesteradmin.test.Api.getJobLog;
import static org.folio.harvesteradmin.test.Api.getScript;
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
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP_2;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP_2_ID;
import static org.folio.harvesteradmin.test.sampleData.Samples.SAMPLE_STEP_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.harvesteradmin.MainVerticle;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.tlib.postgres.testing.TenantPgPoolContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

@SuppressWarnings("JUnitMalformedDeclaration")
@RunWith( VertxUnitRunner.class )
public class HarvesterAdminTestSuite {
  private final Logger logger = LoggerFactory.getLogger( "HarvesterAdminTestSuite" );

  static final String TENANT = "mha_test";
  Vertx vertx;
  private static final int PORT_HARVESTER_ADMIN = 9031;
  public static final Header CONTENT_TYPE_JSON = new Header("Content-Type", "application/json");
  public static final Header CONTENT_TYPE_XML = new Header("Content-Type", "application/xml");
  public static final Header OKAPI_TENANT = new Header ("X-Okapi-Tenant", TENANT);


  public HarvesterAdminTestSuite() {}

  @ClassRule
  public static PostgreSQLContainer<?> postgresSQLContainer = TenantPgPoolContainer.create();

  @Rule
  public final TestName name = new TestName();

  @Before
  public void setUp(TestContext testContext) {
    logger.info("setUp " + name.getMethodName());

    vertx = Vertx.vertx();

    // Register the testContext exception handler to catch assertThat
    vertx.exceptionHandler(testContext.exceptionHandler());
    RestAssured.port = PORT_HARVESTER_ADMIN;

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
    deleteTsasByStepIdPrefix();
    deleteRecordsByIdPrefix(THIS_TRANSFORMATIONS_PATH, "transformations");
    deleteRecordsByIdPrefix(THIS_STEPS_PATH, "transformationSteps");
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
    if (transformationStepAssociations.getJsonArray("transformationStepAssociations").size() > 0) {
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
    postConfigRecord(BASE_TRANSFORMATION_JSON, THIS_TRANSFORMATIONS_PATH, 201);
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 400);
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
    postConfigRecord(harvestable, THIS_HARVESTABLES_PATH, 400);
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
  public void cannotPostConfigsWithAlreadyExistingNames() {
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
    tenantAction(TENANT, new JsonObject()
            .put("module_to", "mod-harvester-admin-0.2.0-SNAPSHOT"));

    RestAssured
        .given()
        .header(OKAPI_TENANT)
        .get("harvester-admin/previous-jobs")
        .then()
        .log().ifValidationFails().statusCode(200).extract().response();

    tenantAction(TENANT, new JsonObject()
        .put("module_from", "mod-harvester-admin-0.2.0-SNAPSHOT")
        .put("purge", true));


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

  void tenantAction(String tenant, JsonObject tenantAttributes) {
    ExtractableResponse<Response> response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
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
        .body("error", is((String) null));

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .delete(location)
        .then().statusCode(204);
  }

}
