package org.folio.harvesteradmin.dataaccess;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.dataaccess.dataconverters.JsonToHarvesterXml;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponse;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseGet;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseGetById;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponsePost;
import org.folio.harvesteradmin.statics.Config;
import org.folio.harvesteradmin.statics.EntityRootNames;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.folio.harvesteradmin.statics.ApiPaths.*;
import static org.folio.okapi.common.HttpResponse.*;

public class HarvesterApiClient
{
    protected final static String HEADER_CONTENT_TYPE = "Content-Type";
    protected final static Logger logger = LogManager.getLogger( HarvesterApiClient.class );
    protected final static int BAD_REQUEST = 400;
    protected final static int INTERNAL_SERVER_ERROR = 500;
    protected final static int NO_CONTENT = 204;
    protected final static int CREATED = 201;
    protected final static int NOT_FOUND = 404;
    protected final static int OK = 200;
    protected final WebClient restClient;
    private static final Map<String, String> rootOfEntityByHarvesterPath = new HashMap<>();
    private static final Map<String, String> rootOfResultSetByHarvesterPath = new HashMap<>();

    static
    {
        rootOfEntityByHarvesterPath.put( HARVESTER_HARVESTABLES_PATH, EntityRootNames.HARVESTABLE_ROOT_PROPERTY );
        rootOfEntityByHarvesterPath.put( HARVESTER_STORAGES_PATH, EntityRootNames.STORAGE_ROOT_PROPERTY );
        rootOfEntityByHarvesterPath.put( HARVESTER_TRANSFORMATIONS_PATH, EntityRootNames.TRANSFORMATION_ROOT_PROPERTY );
        rootOfEntityByHarvesterPath.put( HARVESTER_STEPS_PATH, EntityRootNames.STEP_ROOT_PROPERTY );
        rootOfEntityByHarvesterPath.put( HARVESTER_TRANSFORMATIONS_STEPS_PATH,
                EntityRootNames.TRANSFORMATION_STEP_ROOT_PROPERTY );
        rootOfResultSetByHarvesterPath.put( HARVESTER_HARVESTABLES_PATH,
                EntityRootNames.HARVESTABLE_SET_ROOT_PROPERTY );
        rootOfResultSetByHarvesterPath.put( HARVESTER_STORAGES_PATH, EntityRootNames.STORAGE_SET_ROOT_PROPERTY );
        rootOfResultSetByHarvesterPath.put( HARVESTER_TRANSFORMATIONS_PATH,
                EntityRootNames.TRANSFORMATION_SET_ROOT_PROPERTY );
        rootOfResultSetByHarvesterPath.put( HARVESTER_STEPS_PATH, EntityRootNames.STEP_SET_ROOT_PROPERTY );
        rootOfResultSetByHarvesterPath.put( HARVESTER_TRANSFORMATIONS_STEPS_PATH,
                EntityRootNames.TRANSFORMATION_STEP_SET_ROOT_PROPERTY );
    }


    public HarvesterApiClient( Vertx vertx )
    {
        restClient = WebClient.create( vertx );
    }

    public static String mapToNameOfRootOfResultSet( String harvesterPath )
    {
        return rootOfResultSetByHarvesterPath.get( harvesterPath );
    }

    public void respondWithConfigRecords( RoutingContext routingContext, String harvesterPath )
    {
        logger.debug( "In respondWithConfigRecords, path " + harvesterPath );
        String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
        if ( isNonJsonContentType( routingContext ) )
        {
            responseError( routingContext, BAD_REQUEST,
                    "Only accepts Content-Type application/json, was: " + contentType );
        }
        else
        {
            String query = routingContext.request().getParam( "query" );
            getConfigRecords( harvesterPath, query ).onComplete( get -> {
                if ( get.result().wasOK() )
                {
                    responseJson( routingContext, get.result().statusCode() ).end(
                            get.result().jsonObject().encodePrettily() );
                }
                else
                {
                    logger.error(
                            "GET " + harvesterPath + ( query == null || query.isEmpty() ? "" : "(" + query + ")" ) + " encountered a server error: " + get.result().errorMessage() );
                    responseText( routingContext, get.result().statusCode() ).end( get.result().errorMessage() );
                }
            } );
        }
    }

    public Future<ProcessedHarvesterResponseGet> getConfigRecords( String harvesterPath, String query )
    {
        Promise<ProcessedHarvesterResponseGet> promise = Promise.promise();
        String pathAndQuery = harvesterPath + ( query == null || query.isEmpty() ? "" : "?query=" + URLEncoder.encode(
                query, StandardCharsets.UTF_8 ) );
        restClient.get( Config.harvesterPort, Config.harvesterHost, pathAndQuery ).send(
                ar -> promise.complete( new ProcessedHarvesterResponseGet( ar, harvesterPath, query ) ) );
        return promise.future();
    }

    public void respondWithConfigRecordById( RoutingContext routingContext, String harvesterPath )
    {
        String id = routingContext.request().getParam( "id" );
        String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
        if ( isNonJsonContentType( routingContext ) )
        {
            responseError( routingContext, BAD_REQUEST,
                    "Only accepts Content-Type application/json, was: " + contentType );
        }
        else
        {
            restClient.get( Config.harvesterPort, Config.harvesterHost, harvesterPath + "/" + id ).send( ar -> {
                ProcessedHarvesterResponseGetById response = new ProcessedHarvesterResponseGetById( ar, harvesterPath,
                        id );
                if ( response.wasOK() )
                {
                    responseJson( routingContext, response.statusCode() ).end( response.jsonObject().encodePrettily() );
                }
                else
                {
                    if ( response.wasInternalServerError() )
                    {
                        logger.error(
                                " GET by ID (" + id + ") to " + harvesterPath + " encountered a server error: " + response.errorMessage() );
                    }
                    responseText( routingContext, response.statusCode() ).end( response.errorMessage() );
                }
            } );
        }
    }

    public void putConfigRecordAndRespond( RoutingContext routingContext, String harvesterPath )
    {
        String id = routingContext.request().getParam( "id" );
        JsonObject jsonToPut = routingContext.getBodyAsJson();
        String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
        if ( isNonJsonContentType( routingContext ) )
        {
            responseError( routingContext, BAD_REQUEST,
                    "Only accepts Content-Type application/json, was: " + contentType );
        }
        else
        {
            lookUpHarvesterRecordById( harvesterPath, id ).onComplete(
                    idLookUp -> {    // going to return 404 if not found
                        if ( idLookUp.succeeded() )
                        {
                            ProcessedHarvesterResponse idLookUpResponse = idLookUp.result();
                            if ( idLookUp.result().wasNotFound() )
                            {
                                responseText( routingContext, idLookUpResponse.statusCode() ).end(
                                        idLookUp.result().errorMessage() );
                            }
                            else if ( idLookUp.result().wasOK() )
                            {
                                try
                                {
                                    String xml = JsonToHarvesterXml.convertToHarvesterRecord( jsonToPut,
                                            mapToNameOfRootOfEntity( harvesterPath ) );
                                    restClient.put( Config.harvesterPort, Config.harvesterHost,
                                            harvesterPath + "/" + id ).putHeader( HEADER_CONTENT_TYPE,
                                            "application/xml" ).sendBuffer( Buffer.buffer( xml ), put -> {
                                        if ( put.succeeded() )
                                        {
                                            if ( put.result().statusCode() == NO_CONTENT )
                                            {
                                                responseText( routingContext, put.result().statusCode() ).end( "" );
                                            }
                                            else
                                            {
                                                responseText( routingContext, put.result().statusCode() ).end(
                                                        "There was a problem PUTting to " + harvesterPath + "/" + id + ": " + put.result().statusMessage() );
                                    }
                                }
                                else
                                {
                                    responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                            "There was an error PUTting to " + harvesterPath + "/" + id + ": " + put.cause().getMessage() );
                                }
                            } );
                        }
                        catch ( TransformerException | ParserConfigurationException e )
                        {
                            logger.error( "Error parsing json " + jsonToPut );
                            responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                    "Error parsing json " + jsonToPut );
                        }
                    }
                    else
                    {
                        responseText( routingContext, idLookUpResponse.statusCode() ).end(
                                "There was an error (" + idLookUpResponse.statusCode() + ") looking up " + harvesterPath + "/" + id + " before PUT: " + idLookUpResponse.errorMessage() );
                    }
                }
                else
                {
                    responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                            "Could not look up record " + harvesterPath + "/" + id + " before PUT: " + idLookUp.cause().getMessage() );
                }
            } );
        }
    }

    protected Future<HttpResponse<Buffer>> putConfigRecord( RoutingContext routingContext, JsonObject jsonToPut, String generatedId, String harvesterPath )
    {
        Promise<HttpResponse<Buffer>> promisedResponse = Promise.promise();
        String id = ( generatedId == null ? routingContext.request().getParam( "id" ) : generatedId );
        lookUpHarvesterRecordById( harvesterPath, id ).onComplete( idLookUp -> {    // going to return 404 if not found
            if ( idLookUp.succeeded() )
            {
                if ( idLookUp.result().wasNotFound() )
                {
                    promisedResponse.fail(
                            idLookUp.result().errorMessage() + " Status code: " + idLookUp.result().statusCode() );
                }
                else if ( idLookUp.result().wasOK() )
                {
                    try
                    {
                        String xml = JsonToHarvesterXml.convertToHarvesterRecord( jsonToPut,
                                mapToNameOfRootOfEntity( harvesterPath ) );
                        restClient.put( Config.harvesterPort, Config.harvesterHost,
                                harvesterPath + "/" + id ).putHeader( HEADER_CONTENT_TYPE,
                                "application/xml" ).sendBuffer( Buffer.buffer( xml ), put -> {
                            if ( put.succeeded() )
                            {
                                promisedResponse.complete( put.result() );
                            }
                            else
                            {
                                promisedResponse.fail(
                                        "There was an error PUTting to " + harvesterPath + "/" + id + ": " + put.cause().getMessage() );
                            }
                        } );
                    }
                    catch ( TransformerException | ParserConfigurationException e )
                    {
                        logger.error( "Error parsing json " + jsonToPut );
                        promisedResponse.fail(
                                "There was an error PUTting to " + harvesterPath + "/" + id + ": " + "Error parsing json " + jsonToPut );
                    }
                }
                else
                {
                    promisedResponse.fail(
                            "There was an error (" + idLookUp.result().statusCode() + ") looking up " + harvesterPath + "/" + id + " before PUT: " + idLookUp.result().errorMessage() );
                }
            }
            else
            {
                promisedResponse.fail(
                        "Could not look up record " + harvesterPath + "/" + id + " before PUT: " + idLookUp.cause().getMessage() );
            }
        } );
        return promisedResponse.future();
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
     * @param harvesterPath        the REST path to POST the entity to
     */
    public void postConfigRecordAndRespond( RoutingContext routingContext, String harvesterPath )
    {
        if ( harvesterPath.equals( HARVESTER_TRANSFORMATIONS_STEPS_PATH ) )
        {
            postTsaAndRespond( routingContext );
        }
        else if ( harvesterPath.equals( HARVESTER_TRANSFORMATIONS_PATH ) )
        {
            postTransformationAndRespond( routingContext );
        }
        else
        {
            JsonObject requestJson = routingContext.getBodyAsJson();
            logger.debug( "POST body: " + requestJson.encodePrettily() );
            String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
            if ( isNonJsonContentType( routingContext ) )
            {
                responseError( routingContext, BAD_REQUEST,
                        "Only accepts Content-Type application/json, was: " + contentType );
            }
            else
            {
                String id = requestJson.getString( "id" );
                if ( id == null )
                {
                    doPostAndRetrieveAndRespond( routingContext, requestJson, harvesterPath );
                }
                else
                {
                    lookUpHarvesterRecordById( harvesterPath, id ).onComplete(
                            idLookUp -> {  // going to return 422 if found
                                if ( idLookUp.succeeded() )
                                {
                                    if ( idLookUp.result().wasOK() )
                                    {
                                        responseText( routingContext, 422 ).end(
                                                harvesterPath + "/" + id + " already exists" );
                                    }
                                    else if ( idLookUp.result().wasNotFound() )
                                    {
                                        doPostAndRetrieveAndRespond( routingContext, requestJson, harvesterPath );
                                    }
                                    else
                                    {
                                        responseText( routingContext, idLookUp.result().statusCode() ).end(
                                                "There was an error (" + idLookUp.result().statusCode() + ") looking up " + harvesterPath + "/" + id + " before POST: " + idLookUp.result().errorMessage() );
                                    }
                                }
                                else
                                {
                                    responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                            "Could not look up record " + harvesterPath + "/" + id + " before POST: " + idLookUp.cause().getMessage() );
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
     * @param harvesterPath  The Harvester API path to POST the entity to
     */
    private void doPostAndRetrieveAndRespond( RoutingContext routingContext, JsonObject jsonToPost, String harvesterPath )
    {
        doPostAndRetrieve( jsonToPost, harvesterPath ).onComplete( postAndRetrieve -> {
            ProcessedHarvesterResponsePost response = (ProcessedHarvesterResponsePost) postAndRetrieve.result();
            if ( response.wasCreated() )
            {
                responseJson( routingContext, response.statusCode() ).putHeader( "Location", response.location ).end(
                        response.jsonObject().encodePrettily() );
            }
            else
            {
                responseText( routingContext, response.statusCode() ).end( response.errorMessage() );
            }
        } );
    }

    private void postTransformationAndRespond( RoutingContext routingContext )
    {
        JsonObject requestJson = routingContext.getBodyAsJson();
        logger.debug( "Transformation POST body: " + requestJson.encodePrettily() );
        String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
        if ( isNonJsonContentType( routingContext ) )
        {
            responseError( routingContext, BAD_REQUEST,
                    "Only accepts Content-Type application/json, was: " + contentType );
        }
        else
        {
            String id = requestJson.getString( "id" );
            if ( id != null )
            {
                lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_PATH, id ).onComplete(
                        idLookUp -> { // going to return 422 if found
                            if ( idLookUp.succeeded() )
                            {
                                if ( idLookUp.result().wasOK() )
                                {
                                    responseText( routingContext, 422 ).end(
                                            HARVESTER_TRANSFORMATIONS_PATH + "/" + id + " already exists" );
                                }
                                else if ( idLookUp.result().wasNotFound() )
                                {
                                    doPostTransformationWithoutStepsThenPutWithSteps( routingContext );
                                }
                                else
                                {
                                    responseText( routingContext, idLookUp.result().statusCode() ).end(
                                            "There was an error (" + idLookUp.result().statusCode() + ") looking up " + HARVESTER_TRANSFORMATIONS_PATH + "/" + id + " before POST: " + idLookUp.result().errorMessage() );
                                }
                            }
                            else
                            {
                                responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                        "Could not look up record " + HARVESTER_TRANSFORMATIONS_PATH + "/" + id + " before POST: " + idLookUp.cause().getMessage() );
                            }
                        } );
            }
            else
            {
                // There was no transformation ID in the request JSON
                doPostTransformationWithoutStepsThenPutWithSteps( routingContext );
            }
        }
    }

    private int getRandomInt()
    {
        Random rand = new Random();
        return rand.nextInt( 900000000 ) + 100000000;
    }

    private Future<ProcessedHarvesterResponse> doPostAndRetrieve( JsonObject jsonToPost, String harvesterPath )
    {
        Promise<ProcessedHarvesterResponse> promise = Promise.promise();
        try
        {

            String xml = JsonToHarvesterXml.convertToHarvesterRecord( jsonToPost,
                    mapToNameOfRootOfEntity( harvesterPath ) );
            restClient.post( Config.harvesterPort, Config.harvesterHost, harvesterPath ).putHeader( HEADER_CONTENT_TYPE,
                    "application/xml" ).sendBuffer( Buffer.buffer( xml ), ar -> {
                if ( ar.succeeded() )
                {
                    String location = ar.result().getHeader( "Location" );
                    if ( ar.result().statusCode() == CREATED && location != null )
                    {
                        String idFromLocation = location.split( "/" )[location.split( "/" ).length - 1];
                        lookUpHarvesterRecordById( harvesterPath, idFromLocation ).onComplete(
                                // going to return 500, internal server error if not found, 201, Created if found
                                lookUpNewlyCreatedRecord -> promise.complete(
                                        new ProcessedHarvesterResponsePost( ar, harvesterPath,
                                                lookUpNewlyCreatedRecord.result() ) ) );
                    }
                    else
                    {
                        promise.complete( new ProcessedHarvesterResponsePost( ar, harvesterPath, null ) );
                    }
                }
                else
                {
                    promise.complete( new ProcessedHarvesterResponsePost( ar, harvesterPath, null ) );
                }
            } );
        }
        catch ( TransformerException | ParserConfigurationException e )
        {
            promise.complete( new ProcessedHarvesterResponsePost( INTERNAL_SERVER_ERROR, e.getMessage() ) );
        }
        return promise.future();
    }

    private void postTsaAndRespond( RoutingContext routingContext )
    {
        JsonObject requestJson = routingContext.getBodyAsJson();
        logger.debug( "TSA POST body: " + requestJson.encodePrettily() );
        // Check content type
        String contentType = routingContext.request().getHeader( HEADER_CONTENT_TYPE );
        if ( isNonJsonContentType( routingContext ) )
        {
            responseError( routingContext, BAD_REQUEST,
                    "Only accepts Content-Type application/json, was: " + contentType );
        }
        else
        {
            String id = requestJson.getString( "id" );
            if ( id != null )
            {
                lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_STEPS_PATH, id ).onComplete(
                        idLookUp -> { // going to return 422 if found
                            if ( idLookUp.succeeded() )
                            {
                                if ( idLookUp.result().wasOK() )
                                {
                                    responseText( routingContext, 422 ).end(
                                            HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " already exists" );
                                }
                                else if ( idLookUp.result().wasNotFound() )
                                {
                                    doPostTsaPutTransformationAndRespond( routingContext );
                                }
                                else
                                {
                                    responseText( routingContext, idLookUp.result().statusCode() ).end(
                                            "There was an error (" + idLookUp.result().statusCode() + ") looking up " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " before POST: " + idLookUp.result().errorMessage() );
                                }
                            }
                            else
                            {
                                responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                        "Could not look up record " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + id + " before POST: " + idLookUp.cause().getMessage() );
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

    private void doPostTransformationWithoutStepsThenPutWithSteps( RoutingContext routingContext )
    {
        JsonObject transformationJson = routingContext.getBodyAsJson();
        logger.debug( "About to POST-then-PUT " + transformationJson.encodePrettily() );
        Map<String, String> typeToEmbeddedTypeMap = new HashMap<>();
        typeToEmbeddedTypeMap.put( "CustomTransformStep", "customTransformationStep" );
        typeToEmbeddedTypeMap.put( "XmlTransformStep", "xmlTransformationStep" );
        JsonArray stepsIdsJson = transformationJson.containsKey( "stepAssociations" ) ? transformationJson.getJsonArray(
                "stepAssociations" ).copy() : new JsonArray();
        transformationJson.remove( "stepAssociations" );
        List<Future> stepFutures = new ArrayList<>();
        for ( Object arrayObject : stepsIdsJson )
        {
            JsonObject step = (JsonObject) arrayObject;
            String stepId = step.containsKey( "step" ) ? step.getJsonObject( "step" ).getString(
                    "id" ) : step.getString( "stepId" );
            stepFutures.add( lookUpHarvesterRecordById( HARVESTER_STEPS_PATH, stepId ) );
        }
        CompositeFuture.all( stepFutures ).onComplete( result -> {
            if ( result.succeeded() )
            {
                // found all referenced steps, good to create the transformation
                doPostAndRetrieve( transformationJson, HARVESTER_TRANSFORMATIONS_PATH ).onComplete(
                        transformationPost -> {
                            if ( transformationPost.succeeded() && transformationPost.result().statusCode() == CREATED )
                            {
                                JsonObject createdTransformation = transformationPost.result().jsonObject();
                                createdTransformation.put( "stepAssociations", new JsonArray() );
                                for ( int i = 0; i < result.result().size(); i++ )
                                {
                                    ProcessedHarvesterResponseGetById stepResponse = result.result().resultAt( i );
                                    JsonObject stepJson = stepResponse.jsonObject();
                                    JsonObject tsaJson = new JsonObject();
                                    tsaJson.put( "id", Integer.toString( getRandomInt() ) );
                                    tsaJson.put( "position", Integer.toString( i + 1 ) );
                                    tsaJson.put( "step", new JsonObject() );
                                    tsaJson.getJsonObject( "step" ).put( "entityType",
                                            typeToEmbeddedTypeMap.get( stepJson.getString( "type" ) ) );
                                    tsaJson.getJsonObject( "step" ).put( "id", stepJson.getString( "id" ) );
                                    tsaJson.put( "transformation", createdTransformation.getString( "id" ) );
                                    createdTransformation.getJsonArray( "stepAssociations" ).add( tsaJson );
                                }
                                putConfigRecord( routingContext, createdTransformation,
                                        createdTransformation.getString( "id" ),
                                        HARVESTER_TRANSFORMATIONS_PATH ).onComplete( putResponse -> {
                                    if ( putResponse.succeeded() )
                                    {
                                        lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_PATH,
                                                createdTransformation.getString( "id" ) ).onComplete( lookup -> {
                                            if ( lookup.succeeded() )
                                            {
                                                responseJson( routingContext, CREATED ).putHeader( "Location",
                                                        createdTransformation.getString( "id" ) ).end(
                                                        lookup.result().jsonObject().encodePrettily() );
                                            }
                                    else
                                    {
                                        responseText( routingContext, BAD_REQUEST ).end(
                                                " Failed to POST (with subsequent PUT and GET) of Transformation. Retrieval failed with  " + lookup.result().errorMessage() );
                                    }
                                } );
                            }
                            else
                            {
                                responseText( routingContext, BAD_REQUEST ).end(
                                        " Failed to POST (with subsequent PUT and GET) of Transformation. PUT failed with  " + putResponse.result().statusMessage() );
                            }
                        } );
                    }
                } );
            }
        } );

    }

    private void doPostTsaPutTransformationAndRespond( RoutingContext routingContext )
    {
        JsonObject incomingTsa = routingContext.getBodyAsJson();
        String transformationId = incomingTsa.getString( "transformation" );
        String stepId = incomingTsa.getJsonObject( "step" ).getString( "id" );
        lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_PATH, transformationId ).onComplete(
                checkTransformation -> {
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
                                doPostAndRetrieve( incomingTsa, HARVESTER_TRANSFORMATIONS_STEPS_PATH ).onComplete(
                                        result -> {
                                            if ( result.succeeded() && result.result() != null )
                                            {
                                                JsonObject transformationStepAssociation = result.result().jsonObject();
                                                logger.debug(
                                                        "Posted TSA, got: " + transformationStepAssociation.encodePrettily() );
                                                // Get the transformation
                                                lookUpHarvesterRecordById( HARVESTER_TRANSFORMATIONS_PATH,
                                                        transformationId ).onComplete(
                                                        // going to return 422 if not found
                                                        transformationById -> {
                                                            if ( transformationById.succeeded() && transformationById.result().found() )
                                                            {
                                                                // Insert the tsa in the transformation JSON
                                                                JsonObject transformation = transformationById.result().jsonObject();
                                                                logger.debug(
                                                                        "Got basic transformation " + transformation.encodePrettily() );
                                                                transformation.put( "stepAssociations",
                                                                        insertStepIntoPipeline(
                                                                                transformation.getJsonArray(
                                                                                        "stepAssociations" ),
                                                                                transformationStepAssociation ) );
                                                                try
                                                                {
                                                            // PUT the transformation
                                                            String xml = JsonToHarvesterXml.convertToHarvesterRecord(
                                                                    transformation,
                                                                    EntityRootNames.TRANSFORMATION_ROOT_PROPERTY );
                                                            restClient.put( Config.harvesterPort, Config.harvesterHost,
                                                                    HARVESTER_TRANSFORMATIONS_PATH + "/" + transformationId ).putHeader(
                                                                    HEADER_CONTENT_TYPE, "application/xml" ).sendBuffer(
                                                                    Buffer.buffer( xml ), ar -> {
                                                                        if ( ar.succeeded() )
                                                                        {
                                                                            if ( ar.result().statusCode() == NO_CONTENT )
                                                                            {
                                                                                responseJson( routingContext,
                                                                                        CREATED ).end(
                                                                                        transformationStepAssociation.encodePrettily() );
                                                                            }
                                                                            else
                                                                            {
                                                                                responseText( routingContext,
                                                                                        ar.result().statusCode() ).end(
                                                                                        "There was a problem PUTting to " + HARVESTER_TRANSFORMATIONS_STEPS_PATH + "/" + ": " + ar.result().statusMessage() );
                                                                            }
                                                                        }
                                                                        else
                                                                        {
                                                                            responseText( routingContext,
                                                                                    INTERNAL_SERVER_ERROR ).end(
                                                                                    "There was an error PUTting to " + HARVESTER_TRANSFORMATIONS_PATH + "/" + transformationId + ": " + ar.cause().getMessage() );
                                                                        }
                                                                    } );

                                                        }
                                                        catch ( TransformerException | ParserConfigurationException xe )
                                                        {
                                                            logger.error( "Error parsing json " + transformation );
                                                            responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                                                    "Error parsing json " + transformation );
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

    public void deleteConfigRecordAndRespond( RoutingContext routingContext, String harvesterPath )
    {
        String id = routingContext.request().getParam( "id" );
        logger.debug( "Looking up " + harvesterPath + "/" + id + " before attempting delete" );
        lookUpHarvesterRecordById( harvesterPath, id ).onComplete(
                idLookUp -> {  // going to return 404 if not found, else 204 (no content/deleted)
                    if ( idLookUp.succeeded() )
                    {
                        logger.debug(
                                "Look-up of " + harvesterPath + "/" + id + " complete. Status code: " + idLookUp.result().statusCode() );
                        if ( idLookUp.result().wasNotFound() )
                        {
                            responseText( routingContext, idLookUp.result().statusCode() ).end(
                                    idLookUp.result().errorMessage() );
                        }
                        else if ( idLookUp.result().wasOK() )
                        {
                            restClient.delete( Config.harvesterPort, Config.harvesterHost,
                                    harvesterPath + "/" + id ).send( ar -> {
                                if ( ar.succeeded() )
                                {
                                    if ( ar.result().statusCode() == NO_CONTENT )
                                    {
                                        responseText( routingContext, ar.result().statusCode() ).end(
                                                harvesterPath + "/" + id + " deleted" );
                                    }
                                    else
                                    {
                                        responseText( routingContext, ar.result().statusCode() ).end(
                                                "Could not delete " + harvesterPath + "/" + id + ": " + ar.result().bodyAsString() );
                                    }
                                }
                                else
                                {
                                    responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                            "There was an error deleting " + harvesterPath + "/" + id + ": " + ar.cause().getMessage() );
                                }
                            } );
                        }
                        else
                        {
                            responseText( routingContext, idLookUp.result().statusCode() ).end(
                                    "There was an error (" + idLookUp.result().statusCode() + ") looking up " + harvesterPath + "/" + id + " before DELETE: " + idLookUp.result().errorMessage() );
                        }
                    }
                    else
                    {
                        responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                "Could not look up record " + harvesterPath + "/" + id + " before DELETE: " + idLookUp.cause().getMessage() );
                    }
                } );
    }

    private Future<AsyncResult<HttpResponse<Buffer>>> deleteConfigRecord( RoutingContext routingContext, String harvesterPath, String id )
    {
        Promise<AsyncResult<HttpResponse<Buffer>>> promise = Promise.promise();
        restClient.delete( Config.harvesterPort, Config.harvesterHost, harvesterPath + "/" + id ).send( ar -> {
            if ( ar.succeeded() )
            {
                promise.complete( ar );
            }
            else
            {
                promise.fail( ar.cause() );
            }
        } );
        return promise.future();
    }

    /**
     * If possible, delete all records at the path given in the request
     *
     * @param harvesterPath The path from the request
     */
    public void deleteConfigRecordsAndRespond( RoutingContext routingContext, String harvesterPath )
    {
        logger.debug( "Checks before delete of records at " + harvesterPath );
        switch ( harvesterPath )
        {
            case HARVESTER_HARVESTABLES_PATH:
                noJobsRunning( routingContext ).onComplete( result -> {
                    if ( result.succeeded() )
                    {
                        if ( result.result() )
                        {
                            logger.debug( "No jobs running, can delete all harvestables " );
                            deleteAllRecordsAndRespond( routingContext, harvesterPath, null );
                        }
                        else
                        {
                            responseText( routingContext, BAD_REQUEST ).end(
                                    "Cannot delete all harvest configurations, there are job(s) running" );
                        }
                    }
                    else
                    {
                        responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                "There was a problem checking for running jobs, deletion of harvestables aborted: " + result.cause().getMessage() );
                    }
                } );
                break;
            case HARVESTER_TRANSFORMATIONS_PATH:
                noHarvestablesFound().onComplete( noHarvestables -> {
                    if ( noHarvestables.succeeded() )
                    {
                        if ( noHarvestables.result() )
                        {
                            logger.debug( "No harvest configurations found, Can delete all transformations" );
                            deleteAllRecordsAndRespond( routingContext, harvesterPath, null );
                        }
                        else
                        {
                            responseText( routingContext, BAD_REQUEST ).end(
                                    "Cannot delete all transformation pipelines, there are harvestable(s) potentially using some" );
                        }
                    }
                    else
                    {
                        responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                "There was a problem checking for dependent harvestables, deletion of transformations aborted: " + noHarvestables.cause().getMessage() );
                    }
                } );
                break;
            case HARVESTER_STEPS_PATH:
                noStepAssociationsFound().onComplete( noAssociations -> {
                    if ( noAssociations.succeeded() )
                    {
                        if ( noAssociations.result() )
                        {
                            logger.debug( "No steps associated with transformation pipelines. Can delete all steps" );
                            deleteAllRecordsAndRespond( routingContext, harvesterPath, null );
                        }
                        else
                        {
                            responseText( routingContext, BAD_REQUEST ).end(
                                    "Cannot delete all steps, some are associated with transformation pipelines" );
                        }
                    }
                    else
                    {
                        responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                "There was a problem checking for step associations, deletion of steps aborted: " + noAssociations.cause().getMessage() );
                    }
                } );
                break;
            case HARVESTER_STORAGES_PATH:
                noHarvestablesFound().onComplete( noHarvestables -> {
                    if ( noHarvestables.succeeded() )
                    {
                        if ( noHarvestables.result() )
                        {
                            logger.debug( "No harvest configurations found, Can delete all storages" );
                            deleteAllRecordsAndRespond( routingContext, harvesterPath, null );
                        }
                        else
                        {
                            responseText( routingContext, BAD_REQUEST ).end(
                                    "Cannot delete all storages, there are harvestable(s) potentially using some" );
                        }
                    }
                    else
                    {
                        responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                "There was a problem checking for dependent harvestables, deletion of storages aborted: " + noHarvestables.cause().getMessage() );
                    }
                } );
                break;
            case HARVESTER_TRANSFORMATIONS_STEPS_PATH:
                noJobsRunning( routingContext ).onComplete( result -> {
                    if ( result.succeeded() )
                    {
                        if ( result.result() )
                        {
                            logger.debug( "No jobs are running, can delete all transformation step associations" );
                            deleteAllRecordsAndRespond( routingContext, harvesterPath, null );
                        }
                        else
                        {
                            responseText( routingContext, BAD_REQUEST ).end(
                                    "Cannot delete all transformation step associations, there are job(s) running" );
                        }
                    }
                    else
                    {
                        responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                "There was a problem checking for running jobs, deletion of transformation step associations aborted: " + result.cause().getMessage() );
                    }
                } );
                break;
        }
    }

    private void deleteAllRecordsAndRespond( RoutingContext routingContext, String harvesterPath, String query )
    {

        getConfigRecords( harvesterPath, query ).onComplete( get -> {
            if ( get.result().wasOK() )
            {
                if ( get.result().totalRecords() > 0 )
                {
                    logger.info( "Deleting " + get.result().totalRecords() + " from " + harvesterPath );
                    List<Future> deleteEntityFutures = new ArrayList<>();
                    for ( Object o : get.result().getRecords() )
                    {
                        JsonObject entity = (JsonObject) o;
                        deleteEntityFutures.add(
                                deleteConfigRecord( routingContext, harvesterPath, entity.getString( "id" ) ) );
                    }
                    CompositeFuture.all( deleteEntityFutures ).onComplete( result -> {
                        if ( result.succeeded() )
                        {
                            responseText( routingContext, 204 ).end( "All records at " + harvesterPath + " deleted." );
                        }
                        else
                        {
                            responseText( routingContext, 500 ).end(
                                    "There was a problem deleting records at " + harvesterPath + ": " + result.cause() );
                        }
                    } );
                }
            }
        } );
    }

    private Future<Boolean> noHarvestablesFound()
    {
        logger.debug( "Checking for harvesting configurations" );
        Promise<Boolean> promise = Promise.promise();
        restClient.get( Config.harvesterPort, Config.harvesterHost, HARVESTER_HARVESTABLES_PATH ).send( ar -> {
            ProcessedHarvesterResponseGet response = new ProcessedHarvesterResponseGet( ar, HARVESTER_HARVESTABLES_PATH,
                    null );
            if ( response.wasOK() )
            {
                int records = Integer.parseInt( response.jsonObject().getString( "totalRecords" ) );
                promise.complete( records == 0 );
            }
            else
            {
                logger.error( "There was a problem looking up harvestables: " + response.errorMessage() );
                promise.complete( false );
            }
        } );
        return promise.future();
    }

    private Future<Boolean> noStepAssociationsFound()
    {
        logger.debug( "Checking for step associations" );
        Promise<Boolean> promise = Promise.promise();
        restClient.get( Config.harvesterPort, Config.harvesterHost, HARVESTER_TRANSFORMATIONS_STEPS_PATH ).send( ar -> {
            ProcessedHarvesterResponseGet response = new ProcessedHarvesterResponseGet( ar,
                    HARVESTER_TRANSFORMATIONS_STEPS_PATH, null );
            if ( response.wasOK() )
            {
                int records = Integer.parseInt( response.jsonObject().getString( "totalRecords" ) );
                promise.complete( records == 0 );
            }
            else
            {
                logger.error(
                        "There was a problem looking up transformation-step associations (tsas): " + response.errorMessage() );
                promise.complete( false );
            }
        } );
        return promise.future();
    }

    private Future<Boolean> noJobsRunning( RoutingContext routingContext )
    {
        logger.debug( "Checking for running jobs" );
        String query = "query=currentStatus=RUNNING";
        Promise<Boolean> promise = Promise.promise();
        restClient.get( Config.harvesterPort, Config.harvesterHost, HARVESTER_HARVESTABLES_PATH + "?" + query ).send(
                ar -> {
                    ProcessedHarvesterResponseGet response = new ProcessedHarvesterResponseGet( ar,
                            HARVESTER_HARVESTABLES_PATH, query );
                    if ( response.wasOK() )
                    {
                        int records = Integer.parseInt( response.jsonObject().getString( "totalRecords" ) );
                        promise.complete( records == 0 );
                    }
                    else
                    {
                        logger.error( "There was a problem looking up running jobs: " + response.errorMessage() );
                        promise.complete( false );
                    }
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

    protected Future<ProcessedHarvesterResponseGetById> lookUpHarvesterRecordById( String harvesterPath, String id )
    {
        Promise<ProcessedHarvesterResponseGetById> promise = Promise.promise();
        restClient.get( Config.harvesterPort, Config.harvesterHost, harvesterPath + "/" + id ).send( ar -> {
            ProcessedHarvesterResponseGetById adaptedResponse = new ProcessedHarvesterResponseGetById( ar,
                    harvesterPath, id );
            promise.complete( adaptedResponse );
        } );
        return promise.future();
    }

    /**
     * Get the Harvester's name for the root element of entities from the requested Harvester path. Required for
     * wrapping/unwrapping during data conversions.
     *
     * @param harvesterPath defines the name of root property of an entity
     * @return name of the root element for entities from the Harvester path corresponding to the given request path
     */
    private String mapToNameOfRootOfEntity( String harvesterPath )
    {
        return rootOfEntityByHarvesterPath.get( harvesterPath );
    }

}
