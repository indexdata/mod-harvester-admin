package org.folio.harvesteradmin;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class ProcessedHarvesterResponsePost extends ProcessedHarvesterResponse
{

    public String location;
    public ProcessedHarvesterResponsePost( AsyncResult<HttpResponse<Buffer>> response, String apiPath, ProcessedHarvesterResponseGetById confirmationResponse )
    {
        // Check that POST went well and subsequent GET successfully requested the created entity.
        if ( response.succeeded() )
        {
            if ( response.result().statusCode() == 201 && response.result().getHeader( "Location" ) != null )
            {
                if ( confirmationResponse.statusCode == 200 )
                {
                    statusCode = 201;
                    jsonObject = confirmationResponse.jsonObject;
                    location = jsonObject.getString( "id" );
                }
                else
                {
                    statusCode = 500;
                    errorMessage = "There was a problem confirming that entity POSTed to " + apiPath + " was persisted, confirmation request status " + confirmationResponse.statusCode + ", message: " + confirmationResponse.errorMessage;
                }
            }
            else
            {
                statusCode = 500;
                errorMessage = "Error: Could not obtain location from Harvester response; needed to confirm that POSTed record was persisted: (" + response.result().statusCode() + ") " + response.result().bodyAsString();
            }
        }
        else
        {
            statusCode = 500;
            errorMessage = "POST request to " + apiPath + " failed with: " + response.cause().getMessage();
        }

    }

    public ProcessedHarvesterResponsePost( int statusCode, String errorMessage )
    {
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.bodyAsString = this.bodyAsString == null ? "no response body" : this.bodyAsString;
    }


}
