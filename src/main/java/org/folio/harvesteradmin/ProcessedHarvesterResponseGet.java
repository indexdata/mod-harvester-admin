package org.folio.harvesteradmin;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;

public class ProcessedHarvesterResponseGet extends ProcessedHarvesterResponse
{
    public ProcessedHarvesterResponseGet( AsyncResult<HttpResponse<Buffer>> response, String apiPath, String query )
    {
        if ( response.succeeded() )
        {
            bodyAsString = response.result().bodyAsString();
            int harvesterStatusCode = response.result().statusCode();
            statusCode = harvesterStatusCode;
            if ( harvesterStatusCode == 200 )
            {
                jsonObject = Xml2Json.recordSetXml2json( bodyAsString );
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
            errorMessage = "GET request to " + apiPath + " failed " + ( query != null ? " for query " + query : "" ) + ". Body " + bodyAsString + ". Cause: " + response.cause();
        }
    }
}
