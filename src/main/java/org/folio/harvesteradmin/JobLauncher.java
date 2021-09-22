package org.folio.harvesteradmin;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.dataaccess.HarvesterApiClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.folio.harvesteradmin.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.statics.EntityRootNames.HARVESTABLE_ROOT_PROPERTY;
import static org.folio.okapi.common.HttpResponse.responseJson;
import static org.folio.okapi.common.HttpResponse.responseText;

public class JobLauncher extends HarvesterApiClient
{
    private static final String PROP_NAME = "name";
    private static final String PROP_HARVEST_IMMEDIATELY = "harvestImmediately";
    private static final String PROP_LAST_UPDATED = "lastUpdated";
    private static final String PROP_HARVESTABLE_ID = "harvestableId";
    private static final String PROP_INITIATED = "initiated";
    private static final String TRUE = "true";

    private static SimpleDateFormat format;

    public JobLauncher( Vertx vertx )
    {
        super( vertx );

        format = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
        format.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
    }

    public void startJob( RoutingContext routingContext )
    {

        JsonObject runRequest = routingContext.getBodyAsJson();
        String harvestableId = runRequest.getString( PROP_HARVESTABLE_ID );
        lookUpHarvesterRecordById( HARVESTER_HARVESTABLES_PATH, harvestableId ).onComplete( lookUp -> {
            if ( lookUp.succeeded() )
            {
                if ( lookUp.result().wasNotFound() )
                {
                    responseText( routingContext, NOT_FOUND ).end(
                            "Did not find a harvest configuration with ID " + harvestableId + ". No job started." );
                }
                else if ( lookUp.result().wasOK() )
                {
                    JsonObject harvestConfig = lookUp.result().jsonObject().copy();
                    harvestConfig.put( PROP_HARVEST_IMMEDIATELY, TRUE );
                    harvestConfig.put( PROP_LAST_UPDATED, format.format( new Date() ) );
                    putConfigRecord( routingContext, harvestConfig, harvestableId, HARVESTER_HARVESTABLES_PATH,
                            HARVESTABLE_ROOT_PROPERTY ).onComplete( putResponse -> {
                        if ( putResponse.succeeded() )
                        {
                            JsonObject responseOk = new JsonObject();
                            responseOk.put( PROP_HARVESTABLE_ID, harvestableId );
                            responseOk.put( PROP_NAME, harvestConfig.getString( "name" ) );
                            responseOk.put( PROP_INITIATED, harvestConfig.getString( PROP_LAST_UPDATED ) );
                            responseJson( routingContext, OK ).end( responseOk.encodePrettily() );
                        }
                        else
                        {
                            responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                                    "An error occurred when trying to start job using " + runRequest.encode() + ": " + putResponse.cause().getMessage() );
                        }
                    } );
                }
                else
                {
                    responseText( routingContext, lookUp.result().statusCode() ).end(
                            "A problem occurred when looking for the job to start:" + lookUp.result().errorMessage() );
                }
            }
            else
            {
                responseText( routingContext, INTERNAL_SERVER_ERROR ).end(
                        "Could not look up harvest configuration. Job not started." );
            }
        } );
    }

    public void stopJob( RoutingContext routingContext )
    {

    }

}
