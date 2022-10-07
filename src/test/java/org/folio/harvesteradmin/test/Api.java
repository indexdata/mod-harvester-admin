package org.folio.harvesteradmin.test;

import static org.folio.harvesteradmin.test.HarvesterAdminTestSuite.CONTENT_TYPE_JSON;
import static org.folio.harvesteradmin.test.HarvesterAdminTestSuite.OKAPI_TENANT;
import static org.folio.harvesteradmin.test.sampleData.Samples.sampleId;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

public class Api {

  public static Response postConfigRecord(JsonObject record, String api ) {
    return postConfigRecord(record, api, 201);
  }

  public static Response postConfigRecord( JsonObject record, String api, int expectStatus ) {
    return RestAssured
        .given()
        .body(record.encodePrettily())
        .header(CONTENT_TYPE_JSON)
        .header(OKAPI_TENANT)
        .post(api)
        .then()
        .log().ifValidationFails().statusCode(expectStatus).extract().response();
  }

  public static Response getConfigRecord(String api, int id) {
    return getConfigRecord(api, id, 200);
  }

  public static Response getConfigRecord(String api, int id, int expectStatus) {
    return RestAssured
        .given()
        .header(OKAPI_TENANT)
        .get(api + "/" + sampleId(id))
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
    return RestAssured
        .given()
        .header(OKAPI_TENANT)
        .get(api
            + (query == null ? "" : "?query=" + query ))
        .then()
        .log().ifValidationFails().statusCode(expectStatus).extract().response();
  }

  public static Response deleteConfigRecord(String api, String id, int expectStatus) {
    return RestAssured.given()
        .header(OKAPI_TENANT)
        .delete(api + "/" + id)
        .then()
        .log().ifValidationFails().statusCode(expectStatus).extract().response();

  }

  public static Response putConfigRecord(String api, String id, JsonObject record, int status) {
    return RestAssured.given()
        .header(OKAPI_TENANT)
        .header(CONTENT_TYPE_JSON)
        .body(record.encodePrettily())
        .put(api + "/" + id)
        .then()
        .log().ifValidationFails().statusCode(status).extract().response();
  }

}
