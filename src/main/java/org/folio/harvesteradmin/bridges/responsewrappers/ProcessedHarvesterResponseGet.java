package org.folio.harvesteradmin.bridges.responsewrappers;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.folio.harvesteradmin.bridges.converters.HarvesterXml2Json;

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
                jsonObject = HarvesterXml2Json.convertRecordSetToJson( bodyAsString );
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
