package org.folio.harvesteradmin;

import io.vertx.core.json.JsonObject;

public class RestErrorMessage
{

    private final String message;
    private final String statusCode;
    private final String originMessage;

    public RestErrorMessage( String message, String statusCode, String originMessage )
    {
        this.message = message;
        this.statusCode = statusCode;
        this.originMessage = originMessage;
    }

    public JsonObject getJsonObject()
    {
        JsonObject messageObject = new JsonObject();
        messageObject.put( "message", message );
        messageObject.put( "statusCode", statusCode );
        messageObject.put( "originMessage", originMessage );
        return messageObject;
    }

    public String getJsonString()
    {
        return getJsonObject().encodePrettily();
    }
}
