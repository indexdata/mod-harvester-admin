package org.folio.harvesteradmin;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.dataaccess.HarvesterApiClient;
import org.folio.harvesteradmin.statics.ApiPaths;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.folio.okapi.common.HttpResponse.responseText;

public class ScriptHandler extends HarvesterApiClient
{
    public static final String VALID_STEP_TYPE = "XmlTransformStep";
    public static final String STEP_NAME_KEY = "name";
    public static final String STEP_TYPE_KEY = "type";
    public static final String STEP_SCRIPT_KEY = "script";

    public ScriptHandler( Vertx vertx )
    {
        super( vertx );
    }

    public void handleGetScript( RoutingContext routingContext )
    {
        String tenant = MainVerticle.getTenant( routingContext );
        respondWithScript( routingContext, tenant );
    }

    public void respondWithScript( RoutingContext routingContext, String tenant )
    {
        String id = routingContext.request().getParam( "id" );
        getConfigRecordById( ApiPaths.HARVESTER_STEPS_PATH, id, tenant ).onComplete( getStep -> {
            if ( getStep.result().found() )
            {
                String script = getStep.result().jsonObject().getString( STEP_SCRIPT_KEY );
                script = script.replaceAll( "\\r[\\n]?", System.lineSeparator() );
                responseText( routingContext, 200 ).end( script );
            }
            else
            {
                if ( getStep.result().wasNotFound() )
                {
                    responseText( routingContext, 404 ).end(
                            "Did not find step with ID " + id + " to GET script from" );
                }
                else
                {
                    responseText( routingContext, getStep.result().statusCode() ).end(
                            getStep.result().errorMessage() );
                }

            }
        } );
    }

    public void handlePutScript( RoutingContext routingContext )
    {
        String tenant = MainVerticle.getTenant( routingContext );
        String id = routingContext.request().getParam( "id" );
        String name = routingContext.request().getParam( "name" );
        if ( name == null || name.isEmpty() )
        {
            responseText( routingContext, 400 ).end(
                    "Parameter 'name' is mandatory when putting a script to the step. The value should match the name of the step to PUT to." );
        }
        else
        {
            String script = routingContext.getBodyAsString();
            getConfigRecordById( ApiPaths.HARVESTER_STEPS_PATH, id, tenant ).onComplete( getStep -> {
                if ( getStep.result().found() )
                {
                    JsonObject step = getStep.result().jsonObject();
                    String stepType = step.getString( STEP_TYPE_KEY );
                    String stepName = step.getString( STEP_NAME_KEY );
                    if ( !stepType.equals( VALID_STEP_TYPE ) )
                    {
                        responseText( routingContext, 400 ).end(
                                "Can only PUT scripts to steps of type 'XmlTransformStep', step " + id + ", '" + stepName + "' is '" + stepType + "'" );
                    }
                    else if ( stepName.matches( name.replaceAll( "\\*", ".*" ) ) )
                    {

                        String validationResponse = validateScriptAsXml( script );
                        if ( validationResponse.equals( "OK" ) )
                        {
                            step.put( STEP_SCRIPT_KEY, script );

                            putConfigRecord( routingContext, step, id, ApiPaths.HARVESTER_STEPS_PATH,
                                    tenant ).onComplete( putStep -> {
                                if ( putStep.succeeded() )
                                {
                                    responseText( routingContext, 200 ).end(
                                            "Script updated for step " + id + ", '" + stepName + "'" );
                                }
                                else
                                {
                                    responseText( routingContext, 500 ).end( putStep.cause().getMessage() );
                                }
                            } );
                        }
                        else
                        {
                            responseText( routingContext, 422 ).end(
                                    "Validation of the script as XML failed, error message was: " + validationResponse );
                        }
                    }
                    else
                    {
                        responseText( routingContext, 400 ).end(
                                "A script with ID " + id + " was found, but it's name [" + stepName + "] did not match the name provided [" + name + "]." );
                    }
                }
                else if ( getStep.result().wasNotFound() )
                {
                    responseText( routingContext, 404 ).end( "Did not find step with ID " + id + " to PUT script to" );
                }
                else
                {
                    responseText( routingContext, getStep.result().statusCode() ).end(
                            getStep.result().errorMessage() );
                }
            } );
        }
    }

    public String validateScriptAsXml( String script )
    {
        try
        {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            parser.parse( new ByteArrayInputStream( script.getBytes( StandardCharsets.UTF_8 ) ) );
        }
        catch ( ParserConfigurationException | IOException | SAXException pe )
        {
            return pe.getMessage();
        }
        return "OK";
    }
}
