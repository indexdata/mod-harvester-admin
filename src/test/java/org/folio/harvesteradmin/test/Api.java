package org.folio.harvesteradmin.test;

import static io.restassured.RestAssured.given;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.THIS_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.THIS_STEPS_PATH;
import static org.folio.harvesteradmin.test.HarvesterAdminTestSuiteIT.*;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

public class Api {


  public static Response postConfigRecord(JsonObject record, String api ) {
    return postConfigRecord(record, api, 201);
  }

  public static Response postConfigRecord( JsonObject record, String api, int expectStatus ) {
    return given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
        .body(record.encodePrettily())
        .header(CONTENT_TYPE_JSON)
        .post(api)
        .then()
        .log().ifValidationFails().statusCode(expectStatus).extract().response();
  }

  public static Response getConfigRecord(String api, String id) {
    return getConfigRecord(api, id, 200);
  }

  public static Response getConfigRecord(String api, String id, int expectStatus) {
    return given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
        .get(api + "/" + id)
        .then()
        .log().ifValidationFails().statusCode(expectStatus).extract().response();
  }

  public static JsonObject responseJson(Response response) {
    return new JsonObject(response.body().asString());
  }

  public static Response getConfigRecords(String api, int expectStatus) {
    return getConfigRecords(api, null, expectStatus);
  }

  public static Response getConfigRecords(String api, String query, int expectStatus) {
    return given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
        .get(api
            + (query == null ? "" : "?query=" + query ))
        .then()
        .log().ifValidationFails().statusCode(expectStatus).extract().response();
  }

  public static Response deleteConfigRecord(String api, String id, int expectStatus) {
    return given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
        .delete(api + "/" + id)
        .then()
        .log().ifValidationFails().statusCode(expectStatus).extract().response();
  }

  public static Response putConfigRecord(String api, String id, JsonObject record, int status) {
    return given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
        .header(CONTENT_TYPE_JSON)
        .body(record.encodePrettily())
        .put(api + "/" + id)
        .then()
        .log().ifValidationFails().statusCode(status).extract().response();
  }

  public static Response putScript(String stepId, String stepName, String xsl, int status) {
    return given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
        .header(CONTENT_TYPE_XML)
        .body(xsl)
        .put(THIS_STEPS_PATH + "/" + stepId + "/script?name="+stepName)
        .then()
        .log().ifValidationFails().statusCode(status).extract().response();
  }

  public static Response getScript(String stepId, int status) {
    return given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
        .get(THIS_STEPS_PATH + "/" + stepId + "/script")
        .then()
        .log().ifValidationFails().statusCode(status).extract().response();
  }

  public static Response getJobLog(String harvestableId, int status) {
    return given()
            .port(PORT_HARVESTER_ADMIN)
            .header(OKAPI_TENANT)
        .get(THIS_HARVESTABLES_PATH + "/" + harvestableId + "/log")
        .then()
        .log().ifValidationFails().statusCode(status).extract().response();
  }
}
