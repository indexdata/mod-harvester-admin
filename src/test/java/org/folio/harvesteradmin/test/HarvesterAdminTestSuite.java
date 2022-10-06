package org.folio.harvesteradmin.test;

import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_STORAGES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.THIS_TRANSFORMATIONS_PATH;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
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
  private static final Header CONTENT_TYPE_JSON = new Header("Content-Type", "application/json");
  private static final Header OKAPI_TENANT = new Header ("X-Okapi-Tenant", "diku");
  private static final int BASE_ID = 963000000;
  private static final int BASE_STORAGE_ID = 963100001;
  private static final int BASE_TRANSFORMATION_ID = 963100001;

  private static int id (int subId) {
    return BASE_ID + subId;
  }

  public HarvesterAdminTestSuite()
  {

  }

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
          createBaseTransformationAndStorage();
        }));
  }


  private static void createBaseTransformationAndStorage () {
    JsonObject baseStorageJson  = new JsonObject(
        "{\n"
            + "  \"id\" : \""+ BASE_STORAGE_ID+ "\",\n"
            + "  \"name\": \"BASE_STORAGE\",\n"
            + "  \"description\" : \"Test storage definition\",\n"
            + "  \"type\" : \"inventoryStorage\",\n"
            + "  \"url\" : \"http://10.0.2.2:9130/\",\n"
            + "  \"enabled\" : \"true\",\n"
            + "  \"json\" : {\n"
            + "    \"folioAuthPath\" : \"bl-users/login\",\n"
            + "    \"folioTenant\" : \"diku\",\n"
            + "    \"folioUsername\" : \"diku_admin\",\n"
            + "    \"folioPassword\" : \"admin\",\n"
            + "    \"inventoryUpsertPath\" : \"inventory-upsert-hrid\",\n"
            + "    \"inventoryBatchUpsertPath\" : \"inventory-batch-upsert-hrid\"\n"
            + "  }\n"
            + "}"
    );
    JsonObject baseTransformationJson = new JsonObject(
        "{\n"
            + "  \"name\" : \"BASE_TRANSFORMATION\",\n"
            + "  \"id\" : \"" + BASE_TRANSFORMATION_ID +"\",\n"
            + "  \"description\" : \"Test\",\n"
            + "  \"enabled\" : \"true\",\n"
            + "  \"type\" : \"basicTransformation\"\n"
            + "}\n"
    );
    RestAssured.port = PORT_HARVESTER_ADMIN;
    System.out.println("Create base storage");
    RestAssured
        .given()
        .body(baseStorageJson.encodePrettily())
        .header(CONTENT_TYPE_JSON)
        .header(OKAPI_TENANT)
        .post(THIS_STORAGES_PATH)
        .then()
        .log().ifValidationFails()
        .statusCode(201).extract().response();
    System.out.println("Create base transformation");
    RestAssured
        .given()
        .body(baseTransformationJson.encodePrettily())
        .header(CONTENT_TYPE_JSON)
        .header(OKAPI_TENANT)
        .post(THIS_TRANSFORMATIONS_PATH)
        .then()
        .log().ifValidationFails()
        .statusCode(201).extract().response();
  }

  @After
  public void tearDown(TestContext context) {
    cleanUpTestSamples(context);
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      async.complete();
    }));
  }

  private void cleanUpTestSamples (TestContext context) {
    deleteSampleRecordsWithIdsStartingWith963(context, THIS_HARVESTABLES_PATH, "harvestables");
    deleteSampleRecordsWithIdsStartingWith963(context, THIS_STORAGES_PATH, "storages");
    deleteSampleRecordsWithIdsStartingWith963(context, THIS_TRANSFORMATIONS_PATH, "transformations");
  }


  private void deleteSampleRecordsWithIdsStartingWith963(
      TestContext context, String path, String recordsProperty) {
    RestAssured.port = PORT_HARVESTER_ADMIN;
    Response response =
        RestAssured.given()
            .header(OKAPI_TENANT)
            .get(path + "?query=id=963*")
            .then()
            .log().ifValidationFails()
            .statusCode(200).extract().response();
    JsonObject samples = new JsonObject(response.asString());
    JsonArray sampleRecords = samples.getJsonArray(recordsProperty);
    for (Object o : sampleRecords) {
      String id = ((JsonObject) o).getString("id");
      RestAssured.given()
          .header(OKAPI_TENANT)
          .delete(path + "/" + id)
          .then()
          .log().ifValidationFails().statusCode(204);
    }
  }

  @Test
  public void postingHarvestableWillCreateHarvestable( TestContext testContext )
  {
    final int harvestableId = 1;
    JsonObject harvestable =
        new JsonObject(
            "{\n"
                + "  \"id\": \"" + id(harvestableId) +"\",\n"
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
    RestAssured
        .given()
        .body(harvestable.encodePrettily())
        .header(CONTENT_TYPE_JSON)
        .header(OKAPI_TENANT)
        .post(THIS_HARVESTABLES_PATH)
        .then()
        .log().ifValidationFails().statusCode(201);
    RestAssured
        .given()
        .header(OKAPI_TENANT)
        .get(THIS_HARVESTABLES_PATH + "/" + id(harvestableId))
        .then()
        .log().ifValidationFails().statusCode(200);

  }

}
