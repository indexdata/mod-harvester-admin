package org.folio.harvesteradmin;

import io.vertx.core.json.JsonObject;

/**
 * Base class for<br/> (1) capturing certain unconventional protocols and REST responses from the legacy API and
 * transform them into conventional, FOLIO compliant protocols and responses.<br/> (2) converting the legacy XML
 * response (if any) to FOLIO like JSON.
 */
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
