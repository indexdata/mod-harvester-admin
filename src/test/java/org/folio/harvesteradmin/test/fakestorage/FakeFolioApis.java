package org.folio.harvesteradmin.test.fakestorage;

import io.vertx.core.json.JsonObject;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import static org.folio.harvesteradmin.test.Statics.*;


public class FakeFolioApis {
    public final static String CONFIGURATION_STORAGE_PATH = "/configurations/entries";

    public ConfigurationStorage configurationStorage = new ConfigurationStorage();

    public FakeFolioApis(Vertx vertx, TestContext testContext) {
        configurationStorage.attachToFakeStorage(this);

        Router router = Router.router(vertx);
        router.get(CONFIGURATION_STORAGE_PATH).handler(configurationStorage::getRecords);
        router.get(CONFIGURATION_STORAGE_PATH + "/:id").handler(configurationStorage::getRecordById);
        router.post("/*").handler(BodyHandler.create());
        router.post(CONFIGURATION_STORAGE_PATH).handler(configurationStorage::createRecord);
        router.put("/*").handler(BodyHandler.create());
        router.put(CONFIGURATION_STORAGE_PATH + "/:id").handler(configurationStorage::updateRecord);
        router.delete(CONFIGURATION_STORAGE_PATH + "/:id").handler(configurationStorage::deleteRecord);
        HttpServerOptions so = new HttpServerOptions().setHandle100ContinueAutomatically(true);
        vertx.createHttpServer(so)
                .requestHandler(router)
                .listen(PORT_OKAPI)
                .onComplete(testContext.asyncAssertSuccess());
        RestAssured.port = PORT_OKAPI;
    }

    public static JsonObject getRecordsByQuery(String storagePath, String query) {
        return getRecordsByQuery(storagePath, query, 200);
    }

    public static JsonObject getRecordsByQuery(String storagePath, String query, int expectedResponseCode) {
        Response response = RestAssured.given()
                .baseUri(BASE_URI_OKAPI)
                .get(storagePath + "?" + query)
                .then()
                .log().ifValidationFails()
                .statusCode(expectedResponseCode).extract().response();
        return new JsonObject(response.getBody().asString());
    }

    public static JsonObject getRecordById(String storagePath, String id) {
        return getRecordById(storagePath, id, 200);
    }

    public static JsonObject getRecordById(String storagePath, String id, int expectedResponseCode) {
        Response response =  RestAssured.given()
                .baseUri(BASE_URI_OKAPI)
                .get(storagePath + "/" + id)
                .then()
                .log().ifValidationFails()
                .statusCode(expectedResponseCode).extract().response();
        return new JsonObject(response.getBody().asString());
    }

    public static JsonObject post(String storagePath, JsonObject recordToPOST) {
        return post(storagePath, recordToPOST, 201);
    }

    public static JsonObject post(String storagePath, JsonObject recordToPOST, int expectedResponseCode) {
        Response response = RestAssured.given()
                .baseUri(BASE_URI_OKAPI)
                .body(recordToPOST.toString())
                .post(storagePath)
                .then()
                .log().ifValidationFails()
                .statusCode(expectedResponseCode).extract().response();
        if (response.getContentType().equals("application/json")) {
            return new JsonObject(response.getBody().asString());
        } else {
            return new JsonObject().put("response", response.asString());
        }
    }

    public static void put(String storagePath, JsonObject recordToPUT) {
        put(storagePath, recordToPUT, 204);
    }

    public static void put(String storagePath, JsonObject recordToPUT, int expectedResponseCode) {
        RestAssured.given()
                .baseUri(BASE_URI_OKAPI)
                .body(recordToPUT.toString())
                .put(storagePath + "/" + recordToPUT.getString("id"))
                .then()
                .log().ifValidationFails()
                .statusCode(expectedResponseCode).extract().response();
    }

    public static void delete(String storagePath, String id) {
        delete(storagePath, id, 200);
    }

    public static void delete(String storagePath, String id, int expectedResponseCode) {
        RestAssured.given()
                .baseUri(BASE_URI_OKAPI)
                .delete(storagePath + "/" + id)
                .then()
                .log().ifValidationFails()
                .statusCode(expectedResponseCode);

    }

}