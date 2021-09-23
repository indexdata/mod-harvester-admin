package org.folio.harvesteradmin.dataaccess.responsehandlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.folio.harvesteradmin.dataaccess.dataconverters.HarvesterXml2Json;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessedHarvesterResponseGet extends ProcessedHarvesterResponse
{
    private static final Pattern exceptionDescriptionPattern = Pattern.compile(
            "(Exception Description:.*?[\\n\\r]+.*?[\\n\\r]+)", Pattern.DOTALL );
    public ProcessedHarvesterResponseGet( AsyncResult<HttpResponse<Buffer>> response, String apiPath, String query )
    {
        if ( response.succeeded() )
        {
            bodyAsString = response.result().bodyAsString();
            int harvesterStatusCode = response.result().statusCode();
            statusCode = harvesterStatusCode;
            if ( harvesterStatusCode == 200 )
            {
                jsonObject = HarvesterXml2Json.convertRecordSetToJson( bodyAsString );
            }
            else if ( harvesterStatusCode == 500 && bodyAsString.contains(
                    "An exception occurred while creating a query in EntityManager" ) )
            {
                Matcher m = exceptionDescriptionPattern.matcher( bodyAsString );
                if ( m.find() )
                {
                    errorMessage = "Query failed: " + m.group( 1 ).replaceAll( "&#39;", "'" );
                }
                else
                {
                    errorMessage = "Query failed: " + bodyAsString;
                }
                statusCode = 400;
                jsonObject = new JsonObject();
            }
            else
            {
                errorMessage = "GET request to " + apiPath + ( query != null ? " for query " + query : "" ) + " did not return OK (200) but " + statusCode + ": " + bodyAsString;
                jsonObject = new JsonObject();
            }
        }
        else
        {
            statusCode = 500;
            errorMessage = "GET request to " + apiPath + " failed" + ( query != null ? " for query " + query : "" ) + ". Cause: " + response.cause().getMessage();
        }
    }
}
