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
import io.vertx.ext.web.client.WebClient;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
  public void handleGetStorages( RoutingContext routingContext )
  {
    getRecordsAndRespond( routingContext, HARVESTER_STORAGES_PATH );
  }

  public void handleGetStorageById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_STORAGES_PATH );
  }

  public void handlePutStorage( RoutingContext routingContext )
  {
    putRecordAndRespond( routingContext, HARVESTER_STORAGES_PATH, STORAGE_ROOT_PROPERTY );
  }

  public void handlePostStorage( RoutingContext routingContext )
  {
    postRecordAndRespond( routingContext, HARVESTER_STORAGES_PATH, STORAGE_ROOT_PROPERTY );
  }

  public void handleDeleteStorage( RoutingContext routingContext )
  {
    deleteRecordAndRespond( routingContext, HARVESTER_STORAGES_PATH );
  }

  /**
   * Proxies Harvester's GET /harvester/records/harvestables
   */
  public void handleGetHarvestables( RoutingContext routingCtx )
  {
    getRecordsAndRespond( routingCtx, HARVESTER_HARVESTABLES_PATH );
  }

  public void handleGetHarvestableById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_HARVESTABLES_PATH );
  }

  public void handlePutHarvestable( RoutingContext routingCtx )
  {
    putRecordAndRespond( routingCtx, HARVESTER_HARVESTABLES_PATH, HARVESTABLE_ROOT_PROPERTY );
  }

  public void handlePostHarvestable( RoutingContext routingContext )
  {
    postRecordAndRespond( routingContext, HARVESTER_HARVESTABLES_PATH, HARVESTABLE_ROOT_PROPERTY );
  }

  public void handleDeleteHarvestable( RoutingContext routingContext )
  {
    deleteRecordAndRespond( routingContext, HARVESTER_HARVESTABLES_PATH );
  }

  /**
   * Proxies Harvester's GET /harvester/records/transformations
   */
  public void handleGetTransformations( RoutingContext routingCtx )
  {
    getRecordsAndRespond( routingCtx, HARVESTER_TRANSFORMATIONS_PATH );
  }

  public void handleGetTransformationById( RoutingContext routingContext )
  {
    getRecordByIdAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH );
  }

  public void handlePutTransformation( RoutingContext routingContext )
  {
    putRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH, TRANSFORMATION_ROOT_PROPERTY );
  }

  public void handlePostTransformation( RoutingContext routingContext )
  {
    postRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH, TRANSFORMATION_ROOT_PROPERTY );
  }

  public void handleDeleteTransformation( RoutingContext routingContext )
  {
    deleteRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH );
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
    getRecordByIdAndRespond( routingContext, HARVESTER_STEPS_PATH );
  }

  public void handlePutStep( RoutingContext routingContext )
  {
    putRecordAndRespond( routingContext, HARVESTER_STEPS_PATH, STEP_ROOT_PROPERTY );
  }

  public void handlePostStep( RoutingContext routingContext )
  {
    postRecordAndRespond( routingContext, HARVESTER_STEPS_PATH, STEP_ROOT_PROPERTY );
  }

  public void handleDeleteStep( RoutingContext routingContext )
  {
    deleteRecordAndRespond( routingContext, HARVESTER_STEPS_PATH );
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
    getRecordByIdAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH );
  }

  public void handlePutTransformationStep( RoutingContext routingContext )
  {
    putRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH, TRANSFORMATION_STEP_ROOT_PROPERTY );
  }

  public void handlePostTransformationStep( RoutingContext routingContext )
  {
    postTsasAndRespond( routingContext );
    // postRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH, TRANSFORMATION_STEP_ROOT_PROPERTY );
  }


  public void handleDeleteTransformationStep( RoutingContext routingContext )
  {
    deleteRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH );
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
      String query = routingContext.request().getParam( "query" );
      String pathAndQuery = apiPath + ( query == null || query.isEmpty() ? "" : "?query=" + URLEncoder.encode( query,
              StandardCharsets.UTF_8 ) );
      harvesterClient.get( Config.harvesterPort, Config.harvesterHost, pathAndQuery ).send( ar -> {
        ProcessedHarvesterResponseGet response = new ProcessedHarvesterResponseGet( ar, apiPath, query );
        if ( response.getStatusCode() == 200 )
        {
          responseJson( routingContext, response.getStatusCode() ).end( response.getJsonResponse().encodePrettily() );
        }
        else
        {
          logger.error( "GET " + pathAndQuery + " encountered a server error: " + response.getErrorMessage() );
          responseText( routingContext, response.getStatusCode() ).end( response.getErrorMessage() );
        }
      } );
    }
  }

  private void getRecordByIdAndRespond( RoutingContext routingContext, String apiPath )
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
          responseJson( routingContext, response.getStatusCode() ).end( response.getJsonResponse().encodePrettily() );
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

  private void putRecordAndRespond( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    String id = routingContext.request().getParam( "id" );
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      lookUpHarvesterRecordById( apiPath, id ).onComplete( idLookUp -> {
        if ( idLookUp.succeeded() )
        {
          int idLookUpStatus = idLookUp.result().getStatusCode();
          if ( idLookUpStatus == 404 )
          {
            responseText( routingContext, idLookUpStatus ).end( idLookUp.result().getErrorMessage() );
          }
          else if ( idLookUpStatus == 200 )
          {
            try
            {
              String xml = wrapJsonAndConvertToXml( routingContext.getBodyAsJson(), rootProperty );
              harvesterClient.put( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).putHeader(
                      ApiStatics.HEADER_CONTENT_TYPE, "application/xml" ).sendBuffer( Buffer.buffer( xml ), ar -> {
                if ( ar.succeeded() )
                {
                  if ( ar.result().statusCode() == 204 )
                  {
                    responseText( routingContext, ar.result().statusCode() ).end( "" );
                  }
                  else
                  {
                    responseText( routingContext, ar.result().statusCode() ).end(
                            "There was a problem PUTting to " + apiPath + "/" + id + ": " + ar.result().statusMessage() );
                  }
                }
                else
                {
                  responseText( routingContext, 500 ).end(
                          "There was an error PUTting to " + apiPath + "/" + id + ": " + ar.cause().getMessage() );
                }
              } );
            }
            catch ( TransformerException | ParserConfigurationException e )
            {
              logger.error( "Error parsing json " + routingContext.getBodyAsJson() );
              responseText( routingContext, 500 ).end( "Error parsing json " + routingContext.getBodyAsJson() );
            }
          }
          else
          {
            responseText( routingContext, idLookUpStatus ).end(
                    "There was an error (" + idLookUpStatus + ") looking up " + apiPath + "/" + id + " before PUT: " + idLookUp.result().getErrorMessage() );
          }
        }
        else
        {
          responseText( routingContext, 500 ).end(
                  "Could not look up record " + apiPath + "/" + id + " before PUT: " + idLookUp.cause().getMessage() );
        }
      } );
    }
  }


  private void postRecordAndRespond( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    JsonObject requestJson = routingContext.getBodyAsJson();
    logger.debug( "POST body: " + requestJson.encodePrettily() );
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      String id = requestJson.getString( "id" );
      if ( id == null )
      {
        doPostAndRespond( routingContext, apiPath, rootProperty, null );
      }
      else
      {
        lookUpHarvesterRecordById( apiPath, id ).onComplete( idLookUp -> {
          if ( idLookUp.succeeded() )
          {
            int idLookUpStatus = idLookUp.result().getStatusCode();
            if ( idLookUpStatus == 200 )
            {
              responseText( routingContext, 422 ).end( apiPath + "/" + id + " already exists" );
            }
            else if ( idLookUpStatus == 404 )
            {
              doPostAndRespond( routingContext, apiPath, rootProperty, id );
            }
            else
            {
              responseText( routingContext, idLookUpStatus ).end(
                      "There was an error (" + idLookUpStatus + ") looking up " + apiPath + "/" + id + " before POST: " + idLookUp.result().getErrorMessage() );
            }
          }
          else
          {
            responseText( routingContext, 500 ).end(
                    "Could not look up record " + apiPath + "/" + id + " before POST: " + idLookUp.cause().getMessage() );
          }
        } );
      }
    }
  }

  private void postTsasAndRespond( RoutingContext routingContext )
  {
    // Check content type
    // Check if JSON body has ID
    // If so, check if record with that ID exists
    // If so, respond with 422
    // Check if step exists, if not: respond with 422
    // GET the transformation
    // If the transformation doesn't exist: respond with 422
    // Insert the tsa in the transformation JSON
    // PUT the transformation
    // POST the tsa

    JsonObject requestJson = routingContext.getBodyAsJson();
    logger.debug( "POST body: " + requestJson.encodePrettily() );
    // Check content type
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( !isJsonContentTypeOrNone( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      // Check if JSON body has ID
      String id = requestJson.getString( "id" );
      if ( id != null )
      {
        // If so, check if record with that ID exists
        lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_STEPS_PATH, id ).onComplete( idLookUp -> {
          if ( idLookUp.succeeded() )
          {
            int idLookUpStatus = idLookUp.result().getStatusCode();
            if ( idLookUpStatus == 200 )
            {
              // If so, respond with 422
              responseText( routingContext, 422 ).end(
                      HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " already exists" );
            }
            else if ( idLookUpStatus == 404 )
            {
              // Get the transformation ID from the step association
              String transformationId = requestJson.getJsonObject( "transformationStepAssociation" ).getString(
                      "transformation" );
              // Get the transformation
              lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_PATH, transformationId ).onComplete(
                      transformationById -> {
                        if ( transformationById.succeeded() && transformationById.result().found() )
                        {
                          // Insert the tsa in the transformation JSON
                          JsonObject transformation = transformationById.result().jsonObject;
                          transformation.getJsonObject( "basicTransformation" ).getJsonArray( "stepAssociations" ).add(
                                  requestJson.getJsonObject( "transformationStepAssociation" ) );
                          try
                          {
                            // PUT the transformation
                            String xml = wrapJsonAndConvertToXml( transformation, TRANSFORMATION_ROOT_PROPERTY );
                            harvesterClient.put( Config.harvesterPort, Config.harvesterHost,
                                    HARVESTER_TRANSFORMATIONS_PATH + "/" + transformationId ).putHeader(
                                    ApiStatics.HEADER_CONTENT_TYPE, "application/xml" ).sendBuffer(
                                    Buffer.buffer( xml ), ar -> {
                                      if ( ar.succeeded() )
                                      {
                                        if ( ar.result().statusCode() == 204 )
                                        {
                                          // POST the tsa
                                          doPostAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH,
                                                  TRANSFORMATION_STEP_ROOT_PROPERTY, id );
                                        }
                                        else
                                        {
                                          responseText( routingContext, ar.result().statusCode() ).end(
                                                  "There was a problem PUTting to " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + ": " + ar.result().statusMessage() );
                                        }
                                      }
                                      else
                                      {
                                        responseText( routingContext, 500 ).end(
                                                "There was an error PUTting to " + HARVESTER_TRANSFORMATIONS_PATH + "/" + id + ": " + ar.cause().getMessage() );
                                      }
                                    } );

                          }
                          catch ( TransformerException | ParserConfigurationException xe )
                          {
                            logger.error( "Error parsing json " + transformation );
                            responseText( routingContext, 500 ).end( "Error parsing json " + transformation );
                          }
                        }
                        else
                        {
                          // If the transformation doesn't exist: respond with 422
                          responseText( routingContext, 422 ).end(
                                  "Problem POSTing " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + ", could not retrieve referenced transformation with ID " + transformationId + ": " + transformationById.cause().toString() );
                        }
                      } );
            }
            else
            {
              responseText( routingContext, idLookUpStatus ).end(
                      "There was an error (" + idLookUpStatus + ") looking up " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " before POST: " + idLookUp.result().getErrorMessage() );
            }
          }
          else
          {
            responseText( routingContext, 500 ).end(
                    "Could not look up record " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " before POST: " + idLookUp.cause().getMessage() );
          }
        } );
      }
      else
      {
        // There was no association ID in the request JSON
        // Get the transformation
        String transformationId = requestJson.getJsonObject( "transformationStepAssociation" ).getString(
                "transformation" );
        lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_PATH, transformationId ).onComplete(
                transformationById -> {
                  if ( transformationById.succeeded() && transformationById.result().found() )
                  {
                    // Insert the tsa in the transformation JSON
                    JsonObject transformation = transformationById.result().jsonObject;
                    transformation.getJsonObject( "basicTransformation" ).getJsonArray( "stepAssociations" ).add(
                            requestJson.getJsonObject( "transformationStepAssociation" ) );
                    try
                    {
                      // PUT the transformation
                      String xml = wrapJsonAndConvertToXml( transformation, TRANSFORMATION_ROOT_PROPERTY );
                      harvesterClient.put( Config.harvesterPort, Config.harvesterHost,
                              HARVESTER_TRANSFORMATIONS_PATH + "/" + transformationId ).putHeader(
                              ApiStatics.HEADER_CONTENT_TYPE, "application/xml" ).sendBuffer( Buffer.buffer( xml ),
                              ar -> {
                                if ( ar.succeeded() )
                                {
                                  if ( ar.result().statusCode() == 204 )
                                  {
                                    // POST the tsa
                                    doPostAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH,
                                            TRANSFORMATION_STEP_ROOT_PROPERTY, null );
                                  }
                                  else
                                  {
                                    responseText( routingContext, ar.result().statusCode() ).end(
                                            "There was a problem PUTting to " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + ": " + ar.result().statusMessage() );
                                  }
                                }
                                else
                                {
                                  responseText( routingContext, 500 ).end(
                                          "There was an error PUTting to " + HARVESTER_TRANSFORMATIONS_PATH + "/" + transformationId + ": " + ar.cause().getMessage() );
                                }
                              } );

                    }
                    catch ( TransformerException | ParserConfigurationException xe )
                    {
                      logger.error( "Error parsing json " + transformation );
                      responseText( routingContext, 500 ).end( "Error parsing json " + transformation );
                    }
                  }
                  else
                  {
                    // If the transformation doesn't exist: respond with 422
                    responseText( routingContext, 422 ).end(
                            "Problem POSTing " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + ", could not retrieve referenced transformation with ID " + transformationId + ": " + transformationById.cause() );
                  }
                } );

      }
    }
  }

  private void doPostAndRespond( RoutingContext routingContext, String apiPath, String rootProperty, String id )
  {
    try
    {
      String xml = wrapJsonAndConvertToXml( routingContext.getBodyAsJson(), rootProperty );
      harvesterClient.post( Config.harvesterPort, Config.harvesterHost, apiPath ).putHeader(
              ApiStatics.HEADER_CONTENT_TYPE, "application/xml" ).sendBuffer( Buffer.buffer( xml ), ar -> {
        if ( ar.succeeded() )
        {
          String location = ar.result().getHeader( "Location" );
          if ( ar.result().statusCode() == 201 && location != null )
          {
            String idFromLocation = location.split( "/" )[location.split( "/" ).length - 1];
            lookUpHarvesterRecordById( apiPath, idFromLocation ).onComplete( lookUpNewRecord -> {
              if ( lookUpNewRecord.succeeded() )
              {
                responseJson( routingContext, 201 ).end( lookUpNewRecord.result().getJsonResponse().encodePrettily() );
              }
              else
              {
                responseText( routingContext, 500 ).end(
                        "There was a problem verifying the creation of " + apiPath + "/" + id + ": " + lookUpNewRecord.cause().getMessage() );
              }
            } );
          }
          else
          {
            responseText( routingContext, ar.result().statusCode() ).end(
                    "There was a problem POSTing to " + apiPath + ": " + ar.result().statusMessage() );
          }
        }
        else
        {
          responseText( routingContext, 500 ).end(
                  "There was an error POSTing to " + apiPath + ": " + ar.cause().getMessage() );
        }
      } );
    }
    catch ( TransformerException | ParserConfigurationException e )
    {
      logger.error( "Error parsing json " + routingContext.getBodyAsJson() );
      responseText( routingContext, 500 ).end( "Error parsing json " + routingContext.getBodyAsJson() );
    }
  }

  public void deleteRecordAndRespond( RoutingContext routingContext, String apiPath )
  {
    String id = routingContext.request().getParam( "id" );
    logger.debug( "Looking up " + apiPath + "/" + id + " before attempting delete" );
    lookUpHarvesterRecordById( apiPath, id ).onComplete( idLookUp -> {
      if ( idLookUp.succeeded() )
      {
        int idLookUpStatus = idLookUp.result().getStatusCode();
        logger.debug( "Look-up of " + apiPath + "/" + id + " complete. Status code: " + idLookUpStatus );
        if ( idLookUpStatus == 404 )
        {
          responseText( routingContext, idLookUpStatus ).end( idLookUp.result().getErrorMessage() );
        }
        else if ( idLookUpStatus == 200 )
        {
          harvesterClient.delete( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).send( ar -> {
            if ( ar.succeeded() )
            {
              if ( ar.result().statusCode() == 204 )
              {
                responseText( routingContext, ar.result().statusCode() ).end( apiPath + "/" + id + " deleted" );
              }
              else
              {
                responseText( routingContext, ar.result().statusCode() ).end(
                        "Could not delete " + apiPath + "/" + id + ": " + ar.result().bodyAsString() );
              }
            }
            else
            {
              responseText( routingContext, 500 ).end(
                      "There was an error deleting " + apiPath + "/" + id + ": " + ar.cause().getMessage() );
            }
          } );
        }
        else
        {
          responseText( routingContext, idLookUpStatus ).end(
                  "There was an error (" + idLookUpStatus + ") looking up " + apiPath + "/" + id + " before DELETE: " + idLookUp.result().getErrorMessage() );
        }
      }
      else
      {
        responseText( routingContext, 500 ).end(
                "Could not look up record " + apiPath + "/" + id + " before DELETE: " + idLookUp.cause().getMessage() );
      }
    } );
  }

  private String wrapJsonAndConvertToXml( JsonObject json, String rootProperty ) throws ParserConfigurationException, TransformerException
  {
    JsonObject wrappedObject = new JsonObject();
    wrappedObject.put( rootProperty, json );
    Document doc = Json2Xml.recordJson2harvesterXml( wrappedObject.encode() );
    return Json2Xml.writeXmlDocumentToString( doc );
  }

  private Future<ProcessedHarvesterResponseGetById> lookUpHarvesterRecordById( String apiPath, String id )
  {
    Promise<ProcessedHarvesterResponseGetById> promise = Promise.promise();
    harvesterClient.get( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).send( ar -> {
      ProcessedHarvesterResponseGetById adaptedResponse = new ProcessedHarvesterResponseGetById( ar, apiPath, id );
      promise.complete( adaptedResponse );
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
