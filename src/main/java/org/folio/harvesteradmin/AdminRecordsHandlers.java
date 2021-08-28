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
import static org.folio.okapi.common.HttpResponse.*;

/**
 * Request handlers that proxy Harvester APIs and convert between
 * Harvester XML and FOLIO JSON.
 * @author ne
 */
public class AdminRecordsHandlers {
  private final WebClient harvesterClient;
  private final Logger logger = Logger.getLogger( "harvester-admin" );

  public AdminRecordsHandlers(Vertx vertx)
  {
    harvesterClient = WebClient.create( vertx );
    logger.setLevel( Config.logLevel );
  }

  /**
   * Proxies Harvester's GET /harvester/records/storages
   */
  public void handleGetStorages(RoutingContext routingCtx)
  {
    getRecordsAndRespond( routingCtx, HARVESTER_STORAGES_PATH );
  }

  public void handleGetStorageById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_STORAGES_PATH, STORAGE_ROOT_PROPERTY );
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
  public void handleGetHarvestables(RoutingContext routingCtx)
  {
    getRecordsAndRespond( routingCtx, HARVESTER_HARVESTABLES_PATH );
  }

  public void handleGetHarvestableById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_HARVESTABLES_PATH, HARVESTABLE_ROOT_PROPERTY );
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
  public void handleGetTransformations(RoutingContext routingCtx)
  {
    getRecordsAndRespond( routingCtx, HARVESTER_TRANSFORMATIONS_PATH );
  }

  public void handleGetTransformationById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH, TRANSFORMATION_ROOT_PROPERTY );
  }

  /**
   * Proxies Harvester's GET /harvester/records/steps
   */
  public void handleGetSteps( RoutingContext routingCtx )
  {
    getRecordsAndRespond( routingCtx, HARVESTER_STEPS_PATH );
  }

  public void handleGetStepById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_STEPS_PATH, STEP_ROOT_PROPERTY );
  }

  /**
   * Proxies Harvester's GET /harvester/records/tsas  (transformation - step associations)
   */
  public void handleGetTransformationSteps( RoutingContext routingCtx )
  {
    getRecordsAndRespond( routingCtx, HARVESTER_TRANSFORMATIONS_STEPS_PATH );
  }

  public void handleGetTransformationStepById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH, TRANSFORMATION_STEP_ROOT_PROPERTY );
  }

  private void getRecordsAndRespond( RoutingContext routingContext, String apiPath )
  {
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath ).send( ar -> {
        ProcessedHarvesterResponseGet response = new ProcessedHarvesterResponseGet( ar, apiPath, null );
        if ( response.getStatusCode() == 200 )
        {
          responseJson( routingContext, response.getStatusCode() ).end( response.getJsonResponse().encodePrettily() );
        }
        else
        {
          logger.error( "GET " + apiPath + " encountered a server error: " + response.getErrorMessage() );
          responseText( routingContext, response.getStatusCode() ).end( response.getErrorMessage() );
        }
      } );
    }
  }

  private void getRecordByIdAndRespond( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    String id = routingContext.request().getParam( "id" );
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).send( ar -> {
        ProcessedHarvesterResponseGetById response = new ProcessedHarvesterResponseGetById( ar, apiPath, id );
        if ( response.getStatusCode() == 200 )
        {
          responseJson( routingContext, response.getStatusCode() ).end(
                  response.getJsonResponse().getJsonObject( rootProperty ).encodePrettily() );
        }
        else
        {
          if ( response.getStatusCode() == 500 )
          {
            logger.error(
                    " GET by ID (" + id + ") to " + apiPath + " encountered a server error: " + response.getErrorMessage() );
          }
          responseText( routingContext, response.getStatusCode() ).end( response.getErrorMessage() );
        }
      } );
    }
  }

  private Future<ProcessedHarvesterResponseGetById> getHarvesterRecordById( String apiPath, String id )
  {
    Promise<ProcessedHarvesterResponseGetById> promise = Promise.promise();
    harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).send( ar -> {
      ProcessedHarvesterResponseGetById adaptedResponse = new ProcessedHarvesterResponseGetById( ar, apiPath, id );
      promise.complete( adaptedResponse );
    } );
    return promise.future();
  }

  private Future<JsonObject> putRecord( String apiPath, String id, JsonObject json )
  {
    Buffer buffer = Buffer.buffer( "" );
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

  private String acl( RoutingContext ctx )
  {
    return "acl=" + getTenant( ctx );
  }

  private boolean isJsonContentTypeOrNone( RoutingContext ctx )
  {
    String contentType = ctx.request().getHeader( HEADER_CONTENT_TYPE );
    return ( contentType == null || contentType.startsWith( "application/json" ) );
  }

  private String getTenant( RoutingContext ctx )
  {
    return ctx.request().getHeader( "x-okapi-tenant" );
  }
}
