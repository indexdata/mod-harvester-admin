package org.folio.harvesteradmin.test;

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

import static io.restassured.RestAssured.given;
import static org.folio.harvesteradmin.test.Statics.BASE_URI_OKAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(VertxUnitRunner.class)
public class NoHarvesterTestSuite {
    private static final Logger logger = LoggerFactory.getLogger("HarvesterAdminTestSuite");

    static Vertx vertx;

    private static FakeFolioApis fakeFolioApis;
    static final String TENANT = "mha_test";
    public static final Header OKAPI_TENANT = new Header(XOkapiHeaders.TENANT, TENANT);
    public static final Header OKAPI_URL = new Header(XOkapiHeaders.URL, BASE_URI_OKAPI);
    public static final Header OKAPI_TOKEN = new Header(XOkapiHeaders.TOKEN, "eyJhbGciOiJIUzUxMiJ9eyJzdWIiOiJhZG1pbiIsInVzZXJfaWQiOiI3OWZmMmE4Yi1kOWMzLTViMzktYWQ0YS0wYTg0MDI1YWIwODUiLCJ0ZW5hbnQiOiJ0ZXN0X3RlbmFudCJ9BShwfHcNClt5ZXJ8ImQTMQtAM1sQEnhsfWNmXGsYVDpuaDN3RVQ9");


    public NoHarvesterTestSuite() {
    }

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

    @Test
    public void canGetPreviousJobs() {
        Response response = given().port(Statics.PORT_HARVESTER_ADMIN)
                .header(OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();
        logger.info("canGetPreviousJobs response: " + response.asPrettyString());
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

        FakeFolioApis.post("/configurations/entries",
                new JsonObject()
                        .put("module", "mod-harvester-admin")
                        .put("configName", "PURGE_LOGS_AFTER")
                        .put("value", "2 MONATE"));

        RestAssured
                .given()
                .baseUri("http://localhost:" + Statics.PORT_OKAPI)
                .port(Statics.PORT_OKAPI)
                .header(OKAPI_TENANT)
                .contentType(ContentType.JSON)
                .get("configurations/entries")
                .then().statusCode(200)
                .body("totalRecords", is(1))
                .extract().response();

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

    public static RequestSpecification timeoutConfig(int timeOutInMilliseconds) {
        return new RequestSpecBuilder()
                .setConfig(RestAssured.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", timeOutInMilliseconds)
                                .setParam("http.socket.timeout", timeOutInMilliseconds)))
                .build();
    }

}
