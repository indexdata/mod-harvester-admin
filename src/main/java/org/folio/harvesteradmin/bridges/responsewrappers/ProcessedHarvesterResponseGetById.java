package org.folio.harvesteradmin.bridges.responsewrappers;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.folio.harvesteradmin.bridges.converters.HarvesterXml2Json;


public class ProcessedHarvesterResponseGetById extends ProcessedHarvesterResponse
{

    public ProcessedHarvesterResponseGetById( AsyncResult<HttpResponse<Buffer>> response, String apiPath, String id )
    {
        if ( response.succeeded() )
        {
            bodyAsString = response.result().bodyAsString();
            int harvesterStatusCode = response.result().statusCode();

            if ( harvesterStatusCode == 200 )
            {
                JsonObject transformed = HarvesterXml2Json.convertRecordToJson( bodyAsString );
                if ( transformed == null )
                {
                    errorMessage = "Attempted transformation of the Harvester response [" + bodyAsString + "] failed to produce a JSON object";
                    statusCode = 500;
                }
                else
                {
                    jsonObject = transformed;
                    statusCode = 200;
                }
            }
            else if ( notFound( harvesterStatusCode, bodyAsString ) )
            {
                statusCode = 404;
                errorMessage = apiPath + "/" + id + " not found";
            }
            else
            {
                statusCode = harvesterStatusCode;
                errorMessage = apiPath + "/" + id + " encountered error reported as (" + harvesterStatusCode + ") [" + bodyAsString + "] by the harvester";
            }
        }
        else
        {
            statusCode = 500;
            errorMessage = "GET request to " + apiPath + "/" + id + " failed with: " + response.cause().getMessage();
        }
    }

    private JsonObject getRootObject( JsonObject json )
    {
        Object[] properties = json.fieldNames().toArray();
        if ( properties.length != 1 )
        {
            return null;
        }
        else
        {
            return json.getJsonObject( properties[0].toString() );
        }
    }

    private boolean notFound( int originalStatusCode, String responseBody )
    {
        return ( originalStatusCode == 500 && responseBody.contains( "NullPointerException" ) && responseBody.contains(
                "Converter.getId(" ) );
    }

    public boolean found()
    {
        return ( statusCode == 200 );
    }

}
