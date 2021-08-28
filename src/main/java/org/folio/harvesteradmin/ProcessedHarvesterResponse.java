package org.folio.harvesteradmin;

import io.vertx.core.json.JsonObject;

public abstract class ProcessedHarvesterResponse
{
    protected int statusCode;
    protected String errorMessage;
    protected String bodyAsString;
    protected JsonObject jsonObject = new JsonObject();

    public String getHarvesterResponseBody()
    {
        return bodyAsString;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public JsonObject getJsonResponse()
    {
        return jsonObject;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

}
