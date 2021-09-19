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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

  // Handling STORAGES
  public void handleGetStorages( RoutingContext routingContext )
  {
    respondWithConfigRecords( routingContext, HARVESTER_STORAGES_PATH );
  }

  public void handleGetStorageById( RoutingContext routingContext )
  {
    respondWithConfigRecordById( routingContext, HARVESTER_STORAGES_PATH );
  }

  public void handlePutStorage( RoutingContext routingContext )
  {
    putConfigRecordAndRespond( routingContext, HARVESTER_STORAGES_PATH, STORAGE_ROOT_PROPERTY );
  }

  public void handlePostStorage( RoutingContext routingContext )
  {
    postConfigRecordAndRespond( routingContext, HARVESTER_STORAGES_PATH, STORAGE_ROOT_PROPERTY );
  }

  public void handleDeleteStorage( RoutingContext routingContext )
  {
    deleteAdminRecordAndRespond( routingContext, HARVESTER_STORAGES_PATH );
  }

  // Handling HARVESTABLES
  public void handleGetHarvestables( RoutingContext routingCtx )
  {
    respondWithConfigRecords( routingCtx, HARVESTER_HARVESTABLES_PATH );
  }

  public void handleGetHarvestableById( RoutingContext routingContext )
  {
    respondWithConfigRecordById( routingContext, HARVESTER_HARVESTABLES_PATH );
  }

  public void handlePutHarvestable( RoutingContext routingCtx )
  {
    putConfigRecordAndRespond( routingCtx, HARVESTER_HARVESTABLES_PATH, HARVESTABLE_ROOT_PROPERTY );
  }

  public void handlePostHarvestable( RoutingContext routingContext )
  {
    postConfigRecordAndRespond( routingContext, HARVESTER_HARVESTABLES_PATH, HARVESTABLE_ROOT_PROPERTY );
  }

  public void handleDeleteHarvestable( RoutingContext routingContext )
  {
    deleteAdminRecordAndRespond( routingContext, HARVESTER_HARVESTABLES_PATH );
  }

  // Handling TRANSFORMATIONS
  public void handleGetTransformations( RoutingContext routingCtx )
  {
    respondWithConfigRecords( routingCtx, HARVESTER_TRANSFORMATIONS_PATH );
  }

  public void handleGetTransformationById( RoutingContext routingContext )
  {
    respondWithConfigRecordById( routingContext, HARVESTER_TRANSFORMATIONS_PATH );
  }

  public void handlePutTransformation( RoutingContext routingContext )
  {
    putConfigRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH, TRANSFORMATION_ROOT_PROPERTY );
  }

  public void handlePostTransformation( RoutingContext routingContext )
  {
    postConfigRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH, TRANSFORMATION_ROOT_PROPERTY );
  }

  public void handleDeleteTransformation( RoutingContext routingContext )
  {
    deleteAdminRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_PATH );
  }

  // Handling STEPS
  public void handleGetSteps( RoutingContext routingCtx )
  {
    respondWithConfigRecords( routingCtx, HARVESTER_STEPS_PATH );
  }

  public void handleGetStepById( RoutingContext routingContext )
  {
    respondWithConfigRecordById( routingContext, HARVESTER_STEPS_PATH );
  }

  public void handlePutStep( RoutingContext routingContext )
  {
    putConfigRecordAndRespond( routingContext, HARVESTER_STEPS_PATH, STEP_ROOT_PROPERTY );
  }

  public void handlePostStep( RoutingContext routingContext )
  {
    postConfigRecordAndRespond( routingContext, HARVESTER_STEPS_PATH, STEP_ROOT_PROPERTY );
  }

  public void handleDeleteStep( RoutingContext routingContext )
  {
    deleteAdminRecordAndRespond( routingContext, HARVESTER_STEPS_PATH );
  }

  // Handling TRANSFORMATION-STEP ASSOCIATIONS
  public void handleGetTransformationSteps( RoutingContext routingCtx )
  {
    respondWithConfigRecords( routingCtx, HARVESTER_TRANSFORMATIONS_STEPS_PATH );
  }

  public void handleGetTransformationStepById( RoutingContext routingContext )
  {
    respondWithConfigRecordById( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH );
  }

  public void handlePutTransformationStep( RoutingContext routingContext )
  {
    putConfigRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH,
            TRANSFORMATION_STEP_ROOT_PROPERTY );
  }

  public void handlePostTransformationStep( RoutingContext routingContext )
  {
    postConfigRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH,
            TRANSFORMATION_STEP_ROOT_PROPERTY );
  }

  public void handleDeleteTransformationStep( RoutingContext routingContext )
  {
    deleteAdminRecordAndRespond( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH );
  }


  // Harvester requests

  private void respondWithConfigRecords( RoutingContext routingContext, String apiPath )
  {
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( isNonJsonContentType( routingContext ) )
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

  private void respondWithConfigRecordById( RoutingContext routingContext, String apiPath )
  {
    String id = routingContext.request().getParam( "id" );
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( isNonJsonContentType( routingContext ) )
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

  private void putConfigRecordAndRespond( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    String id = routingContext.request().getParam( "id" );
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( isNonJsonContentType( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      lookUpHarvesterRecordById( apiPath, id ).onComplete( idLookUp -> {    // going to return 404 if not found
        if ( idLookUp.succeeded() )
        {
          ProcessedHarvesterResponse idLookUpResponse = idLookUp.result();
          if ( idLookUpResponse.statusCode == 404 )
          {
            responseText( routingContext, idLookUpResponse.statusCode ).end( idLookUp.result().getErrorMessage() );
          }
          else if ( idLookUpResponse.statusCode == 200 )
          {
            try
            {
              String xml = wrapJsonAndConvertToXml( routingContext.getBodyAsJson(), rootProperty );
              harvesterClient.put( Config.harvesterPort, Config.harvesterHost, apiPath + "/" + id ).putHeader(
                      ApiStatics.HEADER_CONTENT_TYPE, "application/xml" ).sendBuffer( Buffer.buffer( xml ), put -> {
                if ( put.succeeded() )
                {
                  if ( put.result().statusCode() == 204 )
                  {
                    responseText( routingContext, put.result().statusCode() ).end( "" );
                  }
                  else
                  {
                    responseText( routingContext, put.result().statusCode() ).end(
                            "There was a problem PUTting to " + apiPath + "/" + id + ": " + put.result().statusMessage() );
                  }
                }
                else
                {
                  responseText( routingContext, 500 ).end(
                          "There was an error PUTting to " + apiPath + "/" + id + ": " + put.cause().getMessage() );
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
            responseText( routingContext, idLookUpResponse.statusCode ).end(
                    "There was an error (" + idLookUpResponse.statusCode + ") looking up " + apiPath + "/" + id + " before PUT: " + idLookUpResponse.getErrorMessage() );
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

  /**
   * Validates, POSTs, and returns a new harvest configuration record.<br/> <br/> This method proxies the
   * corresponding legacy Harvester end-point to make this API compliant with the conventions of FOLIO APIs (and
   * indeed most REST APIs)<br/> <br/> The legacy API will not respond conventionally when receiving a POST request
   * for an already existing entity, and it will also not return a newly created entity to the caller as e.g. FOLIO
   * APIs do.<br/> <br/> To normalize the REST behavior of POSTs, this method will thus first look up the entity by
   * ID, if an ID is supplied, and return a 422 (unprocessable) if an entity with that ID exists already, otherwise it
   * will go ahead and POST the entity. Next, if the POST is successful, the method will retrieve the persisted entity
   * from the legacy API in order to return it in the response with a status of 201 (created).
   *
   * @param routingContext context
   * @param apiPath        the REST path to POST the entity to
   * @param rootProperty   Name of property that wraps the entity in Harvester
   */
  private void postConfigRecordAndRespond( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    if ( apiPath.equals( HARVESTER_TRANSFORMATIONS_STEPS_PATH ) )
    {
      postTsaAndRespond( routingContext );
    }
    else
    {
      JsonObject requestJson = routingContext.getBodyAsJson();
      logger.debug( "POST body: " + requestJson.encodePrettily() );
      String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
      if ( isNonJsonContentType( routingContext ) )
      {
        responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
      }
      else
      {
        String id = requestJson.getString( "id" );
        if ( id == null )
        {
          doPostAndRetrieveAndRespond( routingContext, apiPath, rootProperty );
        }
        else
        {
          lookUpHarvesterRecordById( apiPath, id ).onComplete( idLookUp -> {  // going to return 422 if found
            if ( idLookUp.succeeded() )
            {
              ProcessedHarvesterResponse idLookUpResponse = idLookUp.result();
              if ( idLookUpResponse.statusCode == 200 )
              {
                responseText( routingContext, 422 ).end( apiPath + "/" + id + " already exists" );
              }
              else if ( idLookUpResponse.statusCode == 404 )
              {
                doPostAndRetrieveAndRespond( routingContext, apiPath, rootProperty );
              }
              else
              {
                responseText( routingContext, idLookUpResponse.statusCode ).end(
                        "There was an error (" + idLookUpResponse.statusCode + ") looking up " + apiPath + "/" + id + " before POST: " + idLookUpResponse.errorMessage );
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
  }

  /**
   * Method POSTs a Harvester configuration record and sends a response containing the newly created record if
   * successful, and otherwise an error message.
   *
   * @param routingContext The context
   * @param apiPath        The Harvester API path to POST the entity to
   * @param rootProperty   The name of wrapping element of the entity in the Harvester's schema
   */
  private void doPostAndRetrieveAndRespond( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    doPostAndRetrieve( routingContext, apiPath, rootProperty ).onComplete( postAndRetrieve -> {
      ProcessedHarvesterResponsePost response = (ProcessedHarvesterResponsePost) postAndRetrieve.result();
      if ( response.statusCode == 201 )
      {
        responseJson( routingContext, response.statusCode ).putHeader( "Location", response.location ).end(
                response.jsonObject.encodePrettily() );
      }
      else
      {
        responseText( routingContext, response.statusCode ).end( response.errorMessage );
      }
    } );
  }

  private Future<ProcessedHarvesterResponse> doPostAndRetrieve( RoutingContext routingContext, String apiPath, String rootProperty )
  {
    Promise<ProcessedHarvesterResponse> promise = Promise.promise();
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
            lookUpHarvesterRecordById( apiPath, idFromLocation ).onComplete(
                    // going to return 500 if not found, 201 if found
                    lookUpNewlyCreatedRecord -> promise.complete(
                            new ProcessedHarvesterResponsePost( ar, apiPath, lookUpNewlyCreatedRecord.result() ) ) );
          }
          else
          {
            promise.complete( new ProcessedHarvesterResponsePost( ar, apiPath, null ) );
          }
        }
        else
        {
          promise.complete( new ProcessedHarvesterResponsePost( ar, apiPath, null ) );
        }
      } );
    }
    catch ( TransformerException | ParserConfigurationException e )
    {
      promise.complete( new ProcessedHarvesterResponsePost( 500, e.getMessage() ) );
    }
    return promise.future();
  }

  private void postTsaAndRespond( RoutingContext routingContext )
  {
    JsonObject requestJson = routingContext.getBodyAsJson();
    logger.debug( "POST body: " + requestJson.encodePrettily() );
    // Check content type
    String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
    if ( isNonJsonContentType( routingContext ) )
    {
      responseError( routingContext, 400, "Only accepts Content-Type application/json, was: " + contentType );
    }
    else
    {
      String id = requestJson.getString( "id" );
      if ( id != null )
      {
        lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_STEPS_PATH, id ).onComplete( idLookUp -> { // going to return 422 if found
          if ( idLookUp.succeeded() )
          {
            int idLookUpStatus = idLookUp.result().getStatusCode();
            if ( idLookUpStatus == 200 )
            {
              responseText( routingContext, 422 ).end( HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " already exists" );
            }
            else if ( idLookUpStatus == 404 )
            {
              doPostTsaPutTransformationAndRespond( routingContext );
            }
            else
            {
              responseText( routingContext, idLookUpStatus ).end( "There was an error (" + idLookUpStatus + ") looking up " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " before POST: " + idLookUp.result().getErrorMessage() );
            }
          }
          else
          {
            responseText( routingContext, 500 ).end( "Could not look up record " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " before POST: " + idLookUp.cause().getMessage() );
          }
        } );
      }
      else
      {
        // There was no association ID in the request JSON
        doPostTsaPutTransformationAndRespond( routingContext );

      }
    }
  }

  private JsonArray insertStepIntoPipeline( JsonArray existingSteps, JsonObject updatingStep )
  {
    logger.debug( "Inserting/moving step: " + updatingStep.encodePrettily() );
    JsonArray updatedListOfSteps = new JsonArray();
    int positionOfUpdatingStep = Integer.parseInt( updatingStep.getString( "position" ) );
    List<JsonObject> existingList = existingSteps.copy().getList();
    for ( JsonObject existingStep : existingList )
    {
      if ( existingStep.getString( "id" ).equals( updatingStep.getString( "id" ) ) )
      {
        existingList.remove( existingStep );
        break;
      }
    }
    existingList.add( Math.min( positionOfUpdatingStep - 1, existingList.size() ), updatingStep.copy() );
    for ( int i = 0; i < existingList.size(); i++ )
    {
      JsonObject step = existingList.get( i );
      step.put( "position", Integer.toString( i + 1 ) );
      updatedListOfSteps.add( step );
    }
    logger.debug( "Constructed new pipeline " + updatedListOfSteps.encodePrettily() );
    return updatedListOfSteps;
  }

  private void doPostTsaPutTransformationAndRespond( RoutingContext routingContext )
  {
    JsonObject incomingTsa = routingContext.getBodyAsJson().getJsonObject( "transformationStepAssociation" );
    String transformationId = incomingTsa.getString( "transformation" );
    String stepId = incomingTsa.getJsonObject( "step" ).getString( "id" );
    lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_PATH, transformationId ).onComplete( checkTransformation -> {
      if ( !checkTransformation.result().found() )
      {
        responseText( routingContext, 422 ).end(
                "Could not create transformation-step association. Transformation " + transformationId + " not found." );
      }
      else
      {
        lookUpHarvesterRecordById( HARVESTER_STEPS_PATH, stepId ).onComplete( checkStep -> {
          if ( !checkStep.result().found() )
          {
            responseText( routingContext, 422 ).end(
                    "Could not create transformation-step association. Step " + stepId + " not found." );
          }
          else
          {
            doPostAndRetrieve( routingContext, HARVESTER_TRANSFORMATIONS_STEPS_PATH,
                    TRANSFORMATION_STEP_ROOT_PROPERTY ).onComplete( result -> {
              if ( result.succeeded() && result.result() != null )
              {
                JsonObject transformationStepAssociation = result.result().jsonObject.getJsonObject(
                        "transformationStepAssociation" );
                logger.debug( "Posted TSA, got: " + transformationStepAssociation.encodePrettily() );
                // Get the transformation
                lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_PATH, transformationId ).onComplete(
                        // going to return 422 if not found
                        transformationById -> {
                          if ( transformationById.succeeded() && transformationById.result().found() )
                          {
                            // Insert the tsa in the transformation JSON
                            JsonObject transformation = transformationById.result().jsonObject;
                            transformation.getJsonObject( "basicTransformation" ).put( "stepAssociations",
                                    insertStepIntoPipeline(
                                            transformation.getJsonObject( "basicTransformation" ).getJsonArray(
                                                    "stepAssociations" ), transformationStepAssociation ) );
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
                                            responseJson( routingContext, 201 ).end(
                                                    transformationStepAssociation.encodePrettily() );
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
              else
              {
                responseText( routingContext, 422 ).end(
                        "Problem POSTing to " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" );
              }
            } );
          }
        } );
      }
    } );
  }

  public void deleteAdminRecordAndRespond( RoutingContext routingContext, String apiPath )
  {
    String id = routingContext.request().getParam( "id" );
    logger.debug( "Looking up " + apiPath + "/" + id + " before attempting delete" );
    lookUpHarvesterRecordById( apiPath, id ).onComplete(
            idLookUp -> {  // going to return 404 if not found, else 204 (deleted)
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

  /**
   * Embeds incoming JSON in two levels of outer objects, see {@link #wrapJson(JsonObject, String)} and converts the
   * result to XML.
   *
   * @param json         Incoming JSON
   * @param rootProperty The top-level property to embed the JSON in
   * @return wrapped JSON converted to an XML string
   */
  private String wrapJsonAndConvertToXml( JsonObject json, String rootProperty ) throws ParserConfigurationException, TransformerException
  {
    JsonObject wrapped = wrapJson( json, rootProperty );
    Document doc = Json2Xml.recordJson2harvesterXml( wrapped );
    return Json2Xml.writeXmlDocumentToString( doc );
  }

  /**
   * Takes incoming JSON and embeds it in two levels of root objects to comply with the Harvester schema.<br/> <br/>
   * For example, the storage entity JSON <br/>
   * <pre>
   * {
   *    "type": "solrStorage",
   *    "id": "10001",
   *    "name": "Local SOLR",
   *    etc
   * }
   * </pre>
   * becomes<br/>
   * <pre>
   * {
   *   "storage": {
   *     "solrStorage": {
   *        "id": "10001",
   *        "name": "Local SOLR",
   *        etc
   *     },
   *     "id": "10001"
   *   }
   * }
   * </pre>
   *
   * @param json         Incoming JSON
   * @param rootProperty The top level property to wrap the incoming JSON in
   * @return The wrapped JSON
   */
  private JsonObject wrapJson( JsonObject json, String rootProperty )
  {
    JsonObject wrappedEntity = new JsonObject();

    String type = json.getString( "type" );
    json.remove( "type" );
    String id = json.getString( "id" );

    JsonObject innerEntity = new JsonObject();
    innerEntity.put( type, json.copy() );
    if ( id != null )
    {
      innerEntity.put( "id", id );
    }

    wrappedEntity.put( rootProperty, innerEntity );
    return wrappedEntity;
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

  private boolean isNonJsonContentType( RoutingContext ctx )
  {
    String contentType = ctx.request().getHeader( HEADER_CONTENT_TYPE );
    return ( contentType != null && !contentType.startsWith( "application/json" ) );
  }

  private String getTenant( RoutingContext ctx )
  {
    return ctx.request().getHeader( "x-okapi-tenant" );
  }
}
