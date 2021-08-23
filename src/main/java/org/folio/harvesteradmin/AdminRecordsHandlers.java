/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;

import static org.folio.harvesteradmin.ApiStatics.*;
import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;

/**
 * Request handlers that proxy Harvester APIs and convert between
 * Harvester XML and FOLIO JSON.
 * @author ne
 */
public class AdminRecordsHandlers {
  private final WebClient harvesterClient;
  private final Logger logger = Logger.getLogger( "harvester-admin" );

  public AdminRecordsHandlers(Vertx vertx) {
    harvesterClient = WebClient.create(vertx);
  }

  /**
   * Proxies Harvester's GET /harvester/records/storages
   */
  public void handleGetStorages(RoutingContext routingCtx) {
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else
    {
      Future<JsonObject> promisedAdminRecords = getRecords( HARVESTER_STORAGES_PATH ); // /?"+acl(routingCtx));
      promisedAdminRecords.onComplete( ar -> responseJson( routingCtx, 200 ).end( ar.result().encodePrettily() ) );
    }
  }

  public void handleGetStorageById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam( "id" );
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else
    {
      Future<JsonObject> promisedAdminRecords = getRecordById( HARVESTER_STORAGES_PATH, id );
      promisedAdminRecords.onComplete( ar -> responseJson( routingCtx, 200 ).end(
              ar.result().getJsonObject( STORAGE_ROOT_PROPERTY ).encodePrettily() ) );
    }
  }

  public void handlePutStorage( RoutingContext routingCtx )
  {
    String id = routingCtx.request().getParam( "id" );
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingCtx ) )
    {
      responseError( routingCtx, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      JsonObject wrappedObject = new JsonObject();
      wrappedObject.put( "storage", routingCtx.getBodyAsJson() );
      Future<JsonObject> promisedPutResponse = putRecord( HARVESTER_STORAGES_PATH, id, wrappedObject );
      promisedPutResponse.onComplete( ar -> {
        if ( ar.succeeded() )
        {
          logger.info( "PUT storage succeeded" );
          responseJson( routingCtx, 204 ).end( "PUT storage succeeded" );
        }
        else
        {
          logger.error( "PUT storage failed: " + ar.cause().getMessage() );
          responseJson( routingCtx, 500 ).end( "PUT storage failed: " + ar.cause().getMessage() );
        }
      } );
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/harvestables
   */
  public void handleGetHarvestables(RoutingContext routingCtx) {
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    String tenantId = getTenant( routingCtx );
    if (!isJsonContentTypeOrNone(routingCtx))  {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else
    {
      Future<JsonObject> promisedAdminRecord = getRecords( HARVESTER_HARVESTABLES_PATH ); //?"+acl(routingCtx));
      promisedAdminRecord.onComplete( ar -> responseJson( routingCtx, 200 ).end( ar.result().encodePrettily() ) );
    }
  }

  public void handleGetHarvestableById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam( "id" );
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else
    {
      Future<JsonObject> promisedAdminRecord = getRecordById( HARVESTER_HARVESTABLES_PATH, id );
      promisedAdminRecord.onComplete( ar -> responseJson( routingCtx, 200 ).end(
              ar.result().getJsonObject( HARVESTABLE_ROOT_PROPERTY ).encodePrettily() ) );
    }
  }

  public void handlePutHarvestable( RoutingContext routingCtx )
  {
    String id = routingCtx.request().getParam( "id" );
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingCtx ) )
    {
      responseError( routingCtx, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      JsonObject wrappedObject = new JsonObject();
      wrappedObject.put( HARVESTABLE_ROOT_PROPERTY, routingCtx.getBodyAsJson() );
      Future<JsonObject> promisedPutResponse = putRecord( HARVESTER_HARVESTABLES_PATH, id, wrappedObject );
      promisedPutResponse.onComplete( ar -> {
        if ( ar.succeeded() )
        {
          logger.debug( "PUT harvestable succeeded" );
          responseJson( routingCtx, 204 ).end( "PUT harvestable succeeded" );
        }
        else
        {
          logger.error( "PUT harvestable failed: " + ar.cause().getMessage() );
          responseJson( routingCtx, 500 ).end( "PUT harvestable failed: " + ar.cause().getMessage() );
        }
      } );
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/transformations
   */
  public void handleGetTransformations(RoutingContext routingCtx) {
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else
    {
      Future<JsonObject> promisedAdminRecords = getRecords( HARVESTER_TRANSFORMATIONS_PATH ); // /?"+acl(routingCtx));
      promisedAdminRecords.onComplete( ar -> responseJson( routingCtx, 200 ).end( ar.result().encodePrettily() ) );
    }
  }

  public void handleGetTransformationById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam( "id" );
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecord = getRecordById( HARVESTER_TRANSFORMATIONS_PATH, id );
      promisedAdminRecord.onComplete( ar -> {
        responseJson( routingCtx, 200 ).end(
                ar.result().getJsonObject( TRANSFORMATION_ROOT_PROPERTY ).encodePrettily() );
      });
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/steps
   */
  public void handleGetSteps(RoutingContext routingCtx) {
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else
    {
      Future<JsonObject> promisedAdminRecords = getRecords( HARVESTER_STEPS_PATH ); // /?"+acl(routingCtx));
      promisedAdminRecords.onComplete( ar -> responseJson( routingCtx, 200 ).end( ar.result().encodePrettily() ) );
    }
  }

  public void handleGetStepById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam( "id" );
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else
    {
      Future<JsonObject> promisedAdminRecord = getRecordById( HARVESTER_STEPS_PATH, id );
      promisedAdminRecord.onComplete( ar -> responseJson( routingCtx, 200 ).end(
              ar.result().getJsonObject( STEP_ROOT_PROPERTY ).encodePrettily() ) );
    }
  }

  /**
   * Proxies Harvester's GET /harvester/records/tsas  (transformation - step associations)
   */
  public void handleGetTransformationSteps(RoutingContext routingCtx) {
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      Future<JsonObject> promisedAdminRecords = getRecords( HARVESTER_TRANSFORMATIONS_STEPS_PATH );
      promisedAdminRecords.onComplete( ar -> {
        responseJson(routingCtx,200).end(ar.result().encodePrettily());
      });
    }
  }

  public void handleGetTransformationStepById(RoutingContext routingCtx) {
    String id = routingCtx.request().getParam( "id" );
    String contentType = routingCtx.request().getHeader( HEADER_CONTENT_TYPE );
    if (!isJsonContentTypeOrNone(routingCtx)) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else
    {
      Future<JsonObject> promisedAdminRecord = getRecordById( HARVESTER_TRANSFORMATIONS_STEPS_PATH, id );
      promisedAdminRecord.onComplete( ar -> responseJson( routingCtx, 200 ).end(
              ar.result().getJsonObject( TRANSFORMATION_STEP_ROOT_PROPERTY ).encodePrettily() ) );
    }
  }

  private boolean isJsonContentTypeOrNone (RoutingContext ctx) {
    String contentType = ctx.request().getHeader( HEADER_CONTENT_TYPE );
    return (contentType == null || contentType.startsWith("application/json"));
  }

  /**
   * Handles GET requests to Harvester's records APIs
   *
   * @param apiPath The sub-path of the particular type of entities to fetch
   * @return Promised JSON object with a list of records or an error response
   */
  private Future<JsonObject> getRecords(String apiPath) {
    Promise<JsonObject> promise = Promise.promise();
    harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath ).send( ar -> {
      String resp;
      JsonObject records = new JsonObject();
      if ( ar.succeeded() )
      {
        HttpResponse<Buffer> harvesterResponse = ar.result();
        if ( harvesterResponse != null )
        {
          resp = harvesterResponse.bodyAsString();
          records = Xml2Json.recordSetXml2json( resp );
        }
        else
        {
          resp = "Response was null";
          records.put( "error", resp );
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
    harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).send( ar -> {
      String resp;
      JsonObject record = new JsonObject();
      if ( ar.succeeded() )
      {
        HttpResponse<Buffer> harvesterResponse = ar.result();
        if ( harvesterResponse != null )
        {
          resp = harvesterResponse.bodyAsString();
          record = Xml2Json.recordXml2Json( resp );
        }
        else
        {
          resp = "Response was null";
          record.put( "error", resp );
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
    try
    {
      doc = Json2Xml.recordJson2xml( json.encode() );
      xmlString = Json2Xml.writeXmlDocumentToString( doc );
      buffer = Buffer.buffer( xmlString == null ? "" : xmlString );
      logger.debug( "Produced XML document [" + xmlString + "] from JSON [" + json.encodePrettily() );
    }
    catch ( DOMException | ParserConfigurationException e )
    {
      logger.error( "Error parsing json " + json.encodePrettily() );
    }
    Promise<JsonObject> promise = Promise.promise();
    harvesterClient.put( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).putHeader(
            ApiStatics.HEADER_CONTENT_TYPE, "application/xml" ).sendBuffer( buffer, ar -> {
      String resp;
      JsonObject record = new JsonObject();
      if ( ar.succeeded() )
      {
        HttpResponse<Buffer> harvesterResponse = ar.result();
        if ( harvesterResponse == null )
        {
          resp = "Response was null";
          record.put( "error", resp );
        }
        else
        {
          if ( harvesterResponse.statusCode() != 204 )
          {
            logger.error(
                    "PUT request encountered a problem: " + harvesterResponse.statusCode() + harvesterResponse.statusMessage() );
            promise.fail(
                    "PUT request to " + apiPath + " encountered a problem: " + harvesterResponse.statusCode() + harvesterResponse.statusMessage() );
          }
        }
      }
      else
      {
        String fail = ar.cause().getMessage();
        logger.debug( "PUT request had an error: " + fail );
        record.put( "error", "PUT to " + apiPath + " failed " + " [" + fail + "]" );
        promise.fail( "PUT to " + apiPath + " failed " + " [" + fail + "]" );
      }
      promise.complete( record );
    } );
    return promise.future();
  }

  private String acl(RoutingContext ctx) {
    return "acl="+getTenant(ctx);
  }

  private String getTenant (RoutingContext ctx) {
    return ctx.request().getHeader("x-okapi-tenant");
  }
}
