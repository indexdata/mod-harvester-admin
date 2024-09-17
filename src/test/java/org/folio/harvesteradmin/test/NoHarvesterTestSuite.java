package org.folio.harvesteradmin.test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.harvesteradmin.MainVerticle;
import org.folio.harvesteradmin.test.fakestorage.FakeFolioApis;
import org.folio.harvesteradmin.test.utils.NetworkUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.tlib.postgres.testing.TenantPgPoolContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.folio.harvesteradmin.test.Statics.timeoutConfig;
import static org.hamcrest.Matchers.is;

@RunWith(VertxUnitRunner.class)
public class NoHarvesterTestSuite {
    private static final Logger logger = LoggerFactory.getLogger("HarvesterAdminTestSuite");

    static Vertx vertx;
    public static int PORT_HARVESTER_ADMIN = NetworkUtils.nextFreePort();

    public NoHarvesterTestSuite() {
    }

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

    @AfterClass
    public static void tearDown(TestContext context) {
        Async async = context.async();
        vertx.close(context.asyncAssertSuccess(res -> async.complete()));
    }

    @Test
    public void canGetPreviousJobs() {
        Statics.initModuleDatabase();
        Response response = given().port(PORT_HARVESTER_ADMIN)
                .header(Statics.OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();
        logger.info("canGetPreviousJobs response: " + response.asPrettyString());
    }

    @Test
    public void willPurgeAgedJobLogsUsingDefaultThreshold() {
        Statics.initModuleDatabase();

        Response response = given().port(PORT_HARVESTER_ADMIN)
                .header(Statics.OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();
        logger.info("will purge jobs response: " + response.asPrettyString());

        given().port(PORT_HARVESTER_ADMIN)
                .header(Statics.OKAPI_TENANT)
                .get("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(200).extract().response();

        LocalDateTime now = LocalDateTime.now();
        final LocalDateTime agedJobStartedTime = now.minusMonths(3).minusDays(1).truncatedTo(ChronoUnit.SECONDS);
        final LocalDateTime agedJobFinishedTime = agedJobStartedTime.plusMinutes(2);
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

        given().port(PORT_HARVESTER_ADMIN).header(Statics.OKAPI_TENANT)
                .body(agedJobJson.encode())
                .contentType(ContentType.JSON)
                .post("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(201).extract().response();

        given().port(PORT_HARVESTER_ADMIN).header(Statics.OKAPI_TENANT)
                .body(newerJobJson.encode())
                .contentType(ContentType.JSON)
                .post("harvester-admin/previous-jobs")
                .then()
                .log().ifValidationFails().statusCode(201).extract().response();

        RestAssured
                .given()
                .port(PORT_HARVESTER_ADMIN)
                .header(Statics.OKAPI_TENANT)
                .contentType(ContentType.JSON)
                .get("harvester-admin/previous-jobs")
                .then().statusCode(200)
                .body("totalRecords", is(2));

        final RequestSpecification timeoutConfig = timeoutConfig(10000);

        given()
                .port(Statics.PORT_OKAPI)
                .header(Statics.OKAPI_TENANT)
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
                .port(PORT_HARVESTER_ADMIN)
                .header(Statics.OKAPI_TENANT)
                .contentType(ContentType.JSON)
                .get("harvester-admin/previous-jobs")
                .then().statusCode(200)
                .body("totalRecords", is(1));

    }

}
