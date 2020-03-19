/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Request handlers that proxy Harvester APIs and convert between
 * Harvester XML and FOLIO JSON.
 * @author ne
 */
public class AdminRecordsHandlers {
  private final WebClient harvesterClient;
  public static final String HARVESTER_HOST_ENV_VAR = "harvester_host";
  public static final String HARVESTER_PORT_ENV_VAR = "harvester_port";
  private final int harvesterPort;
  private final String harvesterHost;
  private final Logger logger = LoggerFactory.getLogger("harvester-admin");

  public AdminRecordsHandlers(Vertx vertx) {
    harvesterClient = WebClient.create(vertx);
    harvesterHost = System.getenv(HARVESTER_HOST_ENV_VAR);
    harvesterPort = Integer.parseInt(System.getenv(HARVESTER_PORT_ENV_VAR));
  }

  /**
   * Proxies Harvester's GET /harvester/records/storages
   * @param routingCtx
   */
  public void handleGetStorages(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("storages");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  public void handleGetStorageById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam("id");
    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecordById("storages",id);
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  public void handlePutStorageById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam("id");
    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      JsonObject storage = routingCtx.getBodyAsJson();
      Future<JsonObject> promisedPutResponse = putRecord("storages", id, storage);
      promisedPutResponse.onComplete( ar -> {
        if (ar.succeeded()) {
          logger.debug("PUT harvestable succeeded");
          responseJson(routingCtx,204).end("PUT storage succeeded");
        } else {
          logger.error("PUT harvestable failed: " + ar.cause().getMessage());
          responseJson(routingCtx,500).end("PUT storage failed: " + ar.cause().getMessage());
        }
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/harvestables
   * @param routingCtx
   */
  public void handleGetHarvestables(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    String tenantId = getTenant(routingCtx);
    if (!isJsonContentTypeOrNone(routingCtx))  {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecord = getRecords("harvestables"); // ?filter=TENANT(" +tenantId +")");
      promisedAdminRecord.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  public void handleGetHarvestableById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam("id");
    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecord = getRecordById("harvestables",id);
      promisedAdminRecord.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  public void handlePutHarvestableById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam("id");
    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      JsonObject harvestable = routingCtx.getBodyAsJson();
      Future<JsonObject> promisedPutResponse = putRecord("harvestables", id, harvestable);
      promisedPutResponse.onComplete( ar -> {
        if (ar.succeeded()) {
          logger.debug("PUT harvestable succeeded");
          responseJson(routingCtx,204).end("PUT harvestable succeeded");
        } else {
          logger.error("PUT harvestable failed: " + ar.cause().getMessage());
          responseJson(routingCtx,500).end("PUT harvestable failed: " + ar.cause().getMessage());
        }
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/transformations
   * @param routingCtx
   */
  public void handleGetTransformations(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("transformations");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  public void handleGetTransformationById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam("id");
    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecord = getRecordById("transformations",id);
      promisedAdminRecord.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/steps
   * @param routingCtx
   */
  public void handleGetSteps(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("steps");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  public void handleGetStepById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam("id");
    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecord = getRecordById("steps",id);
      promisedAdminRecord.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/tsas  (transformation - step associations)
   * @param routingCtx
   */
  public void handleGetTransformationSteps(RoutingContext routingCtx) {

    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords("tsas");
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  public void handleGetTransformationStepById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam("id");
    String contentType = routingCtx.request().getHeader("Content-Type");
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecord = getRecordById("tsas",id);
      promisedAdminRecord.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  private boolean isJsonContentTypeOrNone (RoutingContext ctx) {
    String contentType = ctx.request().getHeader("Content-Type");
    return (contentType == null || contentType.startsWith("application/json"));
  }

  /**
   * Handles GET requests to Harvester's records APIs
   * @param apiPath
   * @return
   */
  private Future<JsonObject> getRecords(String apiPath) {
    Promise<JsonObject> promise = Promise.promise();
    harvesterClient.get(harvesterPort, harvesterHost, "/harvester/records/"+apiPath).send(ar -> {
      String resp;
      JsonObject records = new JsonObject();
      if (ar.succeeded()) {
        HttpResponse<Buffer> harvesterResponse = ar.result();
        if (harvesterResponse != null) {
          resp = harvesterResponse.bodyAsString();
          records = Xml2Json.recordSetXml2json(resp);
        } else {
          resp = "Response was null";
          records.put("error", resp);
        }
        promise.complete(records);
      } else if (ar.failed()) {
        String fail = ar.cause().getMessage();
        records.put("error", "GET " + apiPath + " failed " + " [" + fail + "]");
        promise.complete(records);
      }
    });
    return promise.future();
  }

  private Future<JsonObject> getRecordById(String apiPath, String id) {
    Promise<JsonObject> promise = Promise.promise();
    harvesterClient.get(harvesterPort, harvesterHost, "/harvester/records/"+apiPath+"/"+id).send(ar -> {
      String resp;
      JsonObject record = new JsonObject();
      if (ar.succeeded()) {
        HttpResponse<Buffer> harvesterResponse = ar.result();
        if (harvesterResponse != null) {
          resp = harvesterResponse.bodyAsString();
          record = Xml2Json.recordXml2Json(resp);
        } else {
          resp = "Response was null";
          record.put("error", resp);
        }
        promise.complete(record);
      } else if (ar.failed()) {
        String fail = ar.cause().getMessage();
        record.put("error", "GET " + apiPath + " failed " + " [" + fail + "]");
        promise.complete(record);
      }
    });
    return promise.future();
  }

  private Future<JsonObject> putRecord(String apiPath, String id, JsonObject json) {
    Buffer buffer = Buffer.buffer("");
    String xmlString;
    Document doc;
    try {
      doc = Json2Xml.recordJson2xml(json.encode());
      xmlString = Json2Xml.writeXmlDocumentToString(doc);
      buffer = Buffer.buffer(xmlString == null ? "" : xmlString);
      System.out.println(xmlString);
    } catch (DOMException | ParserConfigurationException e) {
      logger.error("Error parsing json " + json.encodePrettily());
    }
    Promise<JsonObject> promise = Promise.promise();
    harvesterClient.put(harvesterPort, harvesterHost, "/harvester/records/"+apiPath+"/"+id)
            .putHeader("Content-Type", "application/xml")
            .sendBuffer(buffer, ar -> {
      String resp;
      JsonObject record = new JsonObject();
      if (ar.succeeded()) {
        HttpResponse<Buffer> harvesterResponse = ar.result();
        if (harvesterResponse != null) {
        } else {
          resp = "Response was null";
          record.put("error", resp);
        }
        promise.complete(record);
      } else {
        String fail = ar.cause().getMessage();
        record.put("error", "PUT " + apiPath + " failed " + " [" + fail + "]");
        promise.complete(record);
      }
    });
    return promise.future();
  }

  private String getTenant (RoutingContext ctx) {
    return ctx.request().getHeader("x-okapi-tenant");
  }
}
