package org.folio.harvesteradmin.test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.harvesteradmin.MainVerticle;
import org.folio.harvesteradmin.test.fakestorage.FakeFolioApis;
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

import static org.folio.harvesteradmin.test.Statics.BASE_URI_OKAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;

@RunWith(VertxUnitRunner.class)
public class NoHarvesterTest {
    private static final Logger logger = LoggerFactory.getLogger(NoHarvesterTest.class);

    static Vertx vertx;

    private static FakeFolioApis fakeFolioApis;
    static final String TENANT = "mha_test";
    public static final Header OKAPI_TENANT = new Header(XOkapiHeaders.TENANT, TENANT);
    public static final Header OKAPI_URL = new Header(XOkapiHeaders.URL, BASE_URI_OKAPI);
    public static final Header OKAPI_TOKEN = new Header(XOkapiHeaders.TOKEN, "eyJhbGciOiJIUzUxMiJ9eyJzdWIiOiJhZG1pbiIsInVzZXJfaWQiOiI3OWZmMmE4Yi1kOWMzLTViMzktYWQ0YS0wYTg0MDI1YWIwODUiLCJ0ZW5hbnQiOiJ0ZXN0X3RlbmFudCJ9BShwfHcNClt5ZXJ8ImQTMQtAM1sQEnhsfWNmXGsYVDpuaDN3RVQ9");

    @ClassRule
    public static PostgreSQLContainer<?> postgresSQLContainer = TenantPgPoolContainer.create();


    @Rule
    public final TestName name = new TestName();

    @BeforeClass
    public static void beforeClass(TestContext context) {
        vertx = Vertx.vertx();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.baseURI = Statics.BASE_URI_HARVESTER_ADMIN;
        RestAssured.requestSpecification = new RequestSpecBuilder().build();

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setConfig(new JsonObject().put("port", Integer.toString(Statics.PORT_HARVESTER_ADMIN)));
        vertx.deployVerticle(new MainVerticle(), deploymentOptions)
                .onComplete(context.asyncAssertSuccess(x ->
                        fakeFolioApis = new FakeFolioApis(vertx, context)));

    }

    @AfterClass
    public static void afterClass(TestContext context) {
        vertx.close().onComplete(context.asyncAssertSuccess());
    }

    @Before
    public void initSchema() {
        tenantOp(TENANT, new JsonObject()
                        .put("module_to", "mod-harvester-admin-1.0.0")
                , null);
    }

    @After
    public void cleanUpTestRecords() {
        tenantOp(TENANT, new JsonObject()
                .put("module_from", "mod-harvester-admin-1.0.0")
                .put("purge", true), null);
        fakeFolioApis.configurationStorage.wipeMockRecords();
        fakeFolioApis.settingsStorage.wipeMockRecords();
    }

    void tenantOp(String tenant, JsonObject tenantAttributes, String expectedError) {
        ExtractableResponse<Response> response = RestAssured.given()
                .header(XOkapiHeaders.TENANT, tenant)
                .contentType(ContentType.JSON)
                .body(tenantAttributes.encode())
                .post("/_/tenant")
                .then()
                .extract();

        logger.info(response.asString());
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

    public ValidatableResponse canGetPreviousJobs(String parameters) {
      var harvestJob = new JsonObject()
          .put("name", "busy bee")
          .put("harvestableId", 789)
          .put("type", "xmlBulk")
          .put("url", "http://fileserver/xml/")
          .put("allowErrors", true)
          .put("transformation", "12345")
          .put("storage", "Batch Upsert Inventory")
          .put("status", "OK")
          .put("started", "9000-01-01T00:00")
          .put("amountHarvested", 5)
          .put("message", "  a long, long message");
      String[] ends = { "9001-01-01T00:00", "9002-01-01T00:00", "9003-01-01T00:00" };

      for (var finished : ends) {
        harvestJob.put("id", UUID.randomUUID()).put("finished", finished);
        given().port(Statics.PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
            .body(harvestJob.encodePrettily())
            .contentType(ContentType.JSON)
            .post("harvester-admin/previous-jobs")
            .then()
            .statusCode(201);
      }

      return given().port(Statics.PORT_HARVESTER_ADMIN)
          .header(OKAPI_TENANT)
          .get("harvester-admin/previous-jobs" + parameters)
          .then()
          .statusCode(200);
    }

    @Test
    public void canGetPreviousJobs() {
        canGetPreviousJobs("")
        .body("totalRecords", is(3));
    }

    @Test
    public void canGetPreviousJobsFrom() {
        canGetPreviousJobs("?from=9002-01-01T00:00")
        .body("totalRecords", is(2));
    }

    @Test
    public void canGetPreviousJobsUntil() {
        canGetPreviousJobs("?until=9002-12-31T23:59")
        .body("totalRecords", is(2));
    }

    @Test
    public void canGetPreviousJobsFromUntil() {
        canGetPreviousJobs("?from=9002-01-01T00:00&until=9002-12-31T23:59")
        .body("totalRecords", is(1));
    }

    @Test
    public void canGetPreviousJobsLimit() {
        canGetPreviousJobs("?limit=2")
        .body("totalRecords", is(3))
        .body("previousJobs", hasSize(2));
    }

    @Test
    public void canGetPreviousJobsOffset() {
        canGetPreviousJobs("?offset=2")
        .body("totalRecords", is(3))
        .body("previousJobs", hasSize(1));
    }

    @Test
    public void cannotGetPreviousJobInvalidLimit() {
      given().port(Statics.PORT_HARVESTER_ADMIN)
      .header(OKAPI_TENANT)
      .get("harvester-admin/previous-jobs?limit=x")
      .then()
      .statusCode(400);
    }

    @Test
    public void cannotGetPreviousJobInvalidOffset() {
      given().port(Statics.PORT_HARVESTER_ADMIN)
      .header(OKAPI_TENANT)
      .get("harvester-admin/previous-jobs?offset=x")
      .then()
      .statusCode(400);
    }

    @Test
    public void willPurgeAgedJobLogsUsingDefaultThreshold() {
        Response response = given().port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();
        logger.info("will purge jobs response: " + response.asPrettyString());

        given().port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();

        LocalDateTime now = LocalDateTime.now();
        final LocalDateTime agedJobStartedTime = now.minusMonths(3).minusDays(1).truncatedTo(ChronoUnit.SECONDS);
        final LocalDateTime agedJobFinishedTime = agedJobStartedTime.plusMinutes(2);
        final LocalDateTime intermediateJobStartedTime = now.minusMonths(2).minusDays(1).truncatedTo(ChronoUnit.SECONDS);
        final LocalDateTime intermediateJobFinishedTime = intermediateJobStartedTime.plusMinutes(2);
        final LocalDateTime newerJobStartedTime = now.minusMonths(2).truncatedTo(ChronoUnit.SECONDS);
        final LocalDateTime newerJobFinishedTime = newerJobStartedTime.plusMinutes(3);

        JsonObject agedJobJson =
                new JsonObject(
                        "    {\n" +
                                "      \"id\" : \"" + UUID.randomUUID() + "\",\n" +
                                "      \"name\" : \"fake job log\",\n" +
                                "      \"harvestableId\" : 672813240090200,\n" +
                                "      \"type\" : \"xmlBulk\",\n" +
                                "      \"url\" : \"http://fileserver/xml/\",\n" +
                                "      \"allowErrors\" : true,\n" +
                                "      \"transformation\" : \"12345\",\n" +
                                "      \"storage\" : \"Batch Upsert Inventory\",\n" +
                                "      \"status\" : \"OK\",\n" +
                                "      \"started\" : \"" + agedJobStartedTime + "\",\n" +
                                "      \"finished\" : \"" + agedJobFinishedTime + "\",\n" +
                                "      \"amountHarvested\" : 5,\n" +
                                "      \"message\" : \"  Instances_processed/loaded/deletions(signals)/failed:__5___5___0(0)___0_ Holdings_records_processed/loaded/deleted/failed:__13___13___0___0_ Items_processed/loaded/deleted/failed:__4___4___0___0_ Source_records_processed/loaded/deleted/failed:__0___0___0___0_\"\n" +
                                "    }\n");

        JsonObject intermediateJobJson =
                new JsonObject(
                        "    {\n" +
                                "      \"id\" : \"" + UUID.randomUUID() + "\",\n" +
                                "      \"name\" : \"fake job log\",\n" +
                                "      \"harvestableId\" : 672813240090200,\n" +
                                "      \"type\" : \"xmlBulk\",\n" +
                                "      \"url\" : \"http://fileserver/xml/\",\n" +
                                "      \"allowErrors\" : true,\n" +
                                "      \"transformation\" : \"12345\",\n" +
                                "      \"storage\" : \"Batch Upsert Inventory\",\n" +
                                "      \"status\" : \"OK\",\n" +
                                "      \"started\" : \"" + intermediateJobStartedTime + "\",\n" +
                                "      \"finished\" : \"" + intermediateJobFinishedTime + "\",\n" +
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
                                "      \"transformation\" : \"12345\",\n" +
                                "      \"storage\" : \"Batch Upsert Inventory\",\n" +
                                "      \"status\" : \"OK\",\n" +
                                "      \"started\" : \"" + newerJobStartedTime + "\",\n" +
                                "      \"finished\" : \"" + newerJobFinishedTime + "\",\n" +
                                "      \"amountHarvested\" : 3,\n" +
                                "      \"message\" : \"  Instances_processed/loaded/deletions(signals)/failed:__3___3___0(0)___0_ Holdings_records_processed/loaded/deleted/failed:__8___8___0___0_ Items_processed/loaded/deleted/failed:__2___2___0___0_ Source_records_processed/loaded/deleted/failed:__0___0___0___0_\"\n" +
                                "    }\n");

        given().port(Statics.PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
                .body(agedJobJson.encode())
                .contentType(ContentType.JSON)
                .post("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(201).extract().response();

        given().port(Statics.PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
                .body(intermediateJobJson.encode())
                .contentType(ContentType.JSON)
                .post("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(201).extract().response();

        given().port(Statics.PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
                .body(newerJobJson.encode())
                .contentType(ContentType.JSON)
                .post("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(201).extract().response();

        RestAssured
                .given()
                .port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .contentType(ContentType.JSON)
                .get("harvester-admin/previous-jobs")
                .then().statusCode(200)
                .body("totalRecords", is(3));

        final RequestSpecification timeoutConfig = timeoutConfig(10000);

        given()
                .port(Statics.PORT_OKAPI)
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
                .port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .contentType(ContentType.JSON)
                .get("harvester-admin/previous-jobs")
                .then().statusCode(200)
                .body("totalRecords", is(2));

    }

    @Test
    public void willPurgeAgedJobLogsUsingSettingsEntry() {
        Response response = given().port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();
        logger.info("will purge jobs response: " + response.asPrettyString());

        given().port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();

        LocalDateTime now = LocalDateTime.now();
        final LocalDateTime agedJobStartedTime = now.minusMonths(3).minusDays(1).truncatedTo(ChronoUnit.SECONDS);
        final LocalDateTime agedJobFinishedTime = agedJobStartedTime.plusMinutes(2);
        final LocalDateTime intermediateJobStartedTime = now.minusMonths(2).minusDays(1).truncatedTo(ChronoUnit.SECONDS);
        final LocalDateTime intermediateJobFinishedTime = intermediateJobStartedTime.plusMinutes(2);
        final LocalDateTime newerJobStartedTime = now.minusMonths(2).truncatedTo(ChronoUnit.SECONDS);
        final LocalDateTime newerJobFinishedTime = newerJobStartedTime.plusMinutes(3);

        JsonObject agedJobJson =
                new JsonObject(
                        "    {\n" +
                                "      \"id\" : \"" + UUID.randomUUID() + "\",\n" +
                                "      \"name\" : \"fake job log\",\n" +
                                "      \"harvestableId\" : 672813240090200,\n" +
                                "      \"type\" : \"xmlBulk\",\n" +
                                "      \"url\" : \"http://fileserver/xml/\",\n" +
                                "      \"allowErrors\" : true,\n" +
                                "      \"transformation\" : \"12345\",\n" +
                                "      \"storage\" : \"Batch Upsert Inventory\",\n" +
                                "      \"status\" : \"OK\",\n" +
                                "      \"started\" : \"" + agedJobStartedTime + "\",\n" +
                                "      \"finished\" : \"" + agedJobFinishedTime + "\",\n" +
                                "      \"amountHarvested\" : 5,\n" +
                                "      \"message\" : \"  Instances_processed/loaded/deletions(signals)/failed:__5___5___0(0)___0_ Holdings_records_processed/loaded/deleted/failed:__13___13___0___0_ Items_processed/loaded/deleted/failed:__4___4___0___0_ Source_records_processed/loaded/deleted/failed:__0___0___0___0_\"\n" +
                                "    }\n");

        JsonObject intermediateJobJson =
                new JsonObject(
                        "    {\n" +
                                "      \"id\" : \"" + UUID.randomUUID() + "\",\n" +
                                "      \"name\" : \"fake job log\",\n" +
                                "      \"harvestableId\" : 672813240090200,\n" +
                                "      \"type\" : \"xmlBulk\",\n" +
                                "      \"url\" : \"http://fileserver/xml/\",\n" +
                                "      \"allowErrors\" : true,\n" +
                                "      \"transformation\" : \"12345\",\n" +
                                "      \"storage\" : \"Batch Upsert Inventory\",\n" +
                                "      \"status\" : \"OK\",\n" +
                                "      \"started\" : \"" + intermediateJobStartedTime + "\",\n" +
                                "      \"finished\" : \"" + intermediateJobFinishedTime + "\",\n" +
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
                                "      \"transformation\" : \"12345\",\n" +
                                "      \"storage\" : \"Batch Upsert Inventory\",\n" +
                                "      \"status\" : \"OK\",\n" +
                                "      \"started\" : \"" + newerJobStartedTime + "\",\n" +
                                "      \"finished\" : \"" + newerJobFinishedTime + "\",\n" +
                                "      \"amountHarvested\" : 3,\n" +
                                "      \"message\" : \"  Instances_processed/loaded/deletions(signals)/failed:__3___3___0(0)___0_ Holdings_records_processed/loaded/deleted/failed:__8___8___0___0_ Items_processed/loaded/deleted/failed:__2___2___0___0_ Source_records_processed/loaded/deleted/failed:__0___0___0___0_\"\n" +
                                "    }\n");

        given().port(Statics.PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
                .body(agedJobJson.encode())
                .contentType(ContentType.JSON)
                .post("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(201).extract().response();

        given().port(Statics.PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
                .body(intermediateJobJson.encode())
                .contentType(ContentType.JSON)
                .post("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(201).extract().response();

        given().port(Statics.PORT_HARVESTER_ADMIN).header(OKAPI_TENANT)
                .body(newerJobJson.encode())
                .contentType(ContentType.JSON)
                .post("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(201).extract().response();

        RestAssured
                .given()
                .port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .contentType(ContentType.JSON)
                .get("harvester-admin/previous-jobs")
                .then().statusCode(200)
                .body("totalRecords", is(3));

        logger.info(FakeFolioApis.post("/settings/entries",
                new JsonObject()
                        .put("id", UUID.randomUUID().toString())
                        .put("scope", "mod-harvester-admin")
                        .put("key", "PURGE_LOGS_AFTER")
                        .put("value", "2 MONATE")).encodePrettily());


        Response responsex = RestAssured
                .given()
                .baseUri("http://localhost:" + Statics.PORT_OKAPI)
                .port(Statics.PORT_OKAPI)
                .header(OKAPI_TENANT)
                .contentType(ContentType.JSON)
                .get("settings/entries")
                .then().statusCode(200)
                .body("totalRecords", is(1))
                .extract().response();

        logger.info(responsex.asPrettyString());

        final RequestSpecification timeoutConfig = timeoutConfig(10000);

        given()
                .port(Statics.PORT_OKAPI)
                .header(OKAPI_TENANT)
                .header(Statics.OKAPI_URL)
                .header(Statics.OKAPI_TOKEN)
                .contentType(ContentType.JSON)
                .header(XOkapiHeaders.REQUEST_ID, "purge-aged-logs")
                .spec(timeoutConfig)
                .when().post("/harvester-admin/purge-aged-logs")
                .then().log().ifValidationFails().statusCode(204)
                .extract().response();

        RestAssured
                .given()
                .port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .contentType(ContentType.JSON)
                .get("harvester-admin/previous-jobs")
                .then().statusCode(200)
                .body("totalRecords", is(1));
    }

    @Test
    public void willPurgeAgedJobLogsUsingConfigurationsEntry() {
        Response response = given().port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();
        logger.info("will purge jobs response: " + response.asPrettyString());

        given().port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();

        UUID[] ids = { UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID() };
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime[] started = {
            now.minusMonths(3).minusDays(1).truncatedTo(ChronoUnit.SECONDS),
            now.minusMonths(2).minusDays(1).truncatedTo(ChronoUnit.SECONDS),
            now.minusMonths(2).truncatedTo(ChronoUnit.SECONDS)
        };
        for (int i = 0; i < 3; i++) {
          var harvestJobId = ids[i];
          var job = new JsonObject()
              .put("id", harvestJobId)
              .put("name", "foo")
              .put("type", "xmlBulk")
              .put("url", "http://fileserver/xml/")
              .put("transformation", "789")
              .put("storage", "Batch Upsert Inventory")
              .put("harvestableId", 123)
              .put("started", started[i].toString());
          given().body(job.encode())
              .post("harvester-admin/previous-jobs")
              .then().statusCode(201);
          given()
              .header(Statics.CONTENT_TYPE_TEXT)
              .body("2024-01-01T00:00:00.000 INFO [foo (bar)] abcdefghijklmnoopqrstuvwxyzabcdefghijklmnoopqrstuvwxyzabcdefghijklmnoopqrstuvwxyz")
              .post("harvester-admin/previous-jobs/" + harvestJobId + "/log")
              .then().statusCode(201);
          var failedRecords = new JsonArray()
              .add(new JsonObject()
                  .put("id", UUID.randomUUID().toString())
                  .put("timeStamp", "2024-01-01T00:00:00.000")
                  .put("originalRecord", "orig")
                  .put("transformedRecord", new JsonObject())
                  .put("recordErrors", new JsonArray()));
          given()
              .body(new JsonObject().put("failedRecords", failedRecords).encode())
              .post("harvester-admin/previous-jobs/" + harvestJobId + "/failed-records")
              .then().statusCode(201);
          given()
              .get("harvester-admin/previous-jobs/" + harvestJobId + "/log")
              .then().statusCode(200);
          given()
              .get("harvester-admin/previous-jobs/" + harvestJobId + "/failed-records")
              .then().statusCode(200)
              .body("failedRecords.size()", is(1));
        }

        given()
            .get("harvester-admin/previous-jobs")
            .then().statusCode(200)
            .body("totalRecords", is(3));

        FakeFolioApis.post("/configurations/entries",
                new JsonObject()
                        .put("module", "mod-harvester-admin")
                        .put("configName", "PURGE_LOGS_AFTER")
                        .put("value", "2 MONATE"));

        given()
            .baseUri("http://localhost:" + Statics.PORT_OKAPI)
            .get("configurations/entries")
            .then().statusCode(200)
            .body("totalRecords", is(1));

        given()
            .spec(timeoutConfig(10000))
            .when().post("/harvester-admin/purge-aged-logs")
            .then().statusCode(204);

        given()
            .get("harvester-admin/previous-jobs")
            .then().statusCode(200)
            .body("totalRecords", is(1));

        for (int i = 0; i < 2; i++) {
          var harvestJobId = ids[i];
          given()
              .get("harvester-admin/previous-jobs/" + harvestJobId + "/log")
              .then().statusCode(404);
          given()
              .get("harvester-admin/previous-jobs/" + harvestJobId + "/failed-records")
              .then().statusCode(200)
              .body("failedRecords.size()", is(0));
        }
  }

    public static RequestSpecification timeoutConfig(int timeOutInMilliseconds) {
        return new RequestSpecBuilder()
                .setConfig(RestAssured.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", timeOutInMilliseconds)
                                .setParam("http.socket.timeout", timeOutInMilliseconds)))
                .build();
    }

    private static RequestSpecification given() {
      return RestAssured.given()
          .header(OKAPI_TENANT)
          .header(Statics.OKAPI_URL)
          .header(Statics.OKAPI_TOKEN)
          .header(XOkapiHeaders.REQUEST_ID, NoHarvesterTest.class.getName())
          .contentType(ContentType.JSON);
    }
}
