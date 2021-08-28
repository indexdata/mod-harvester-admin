package org.folio.harvesteradmin;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class ProcessedHarvesterResponseGetById extends ProcessedHarvesterResponse
{

    public ProcessedHarvesterResponseGetById( AsyncResult<HttpResponse<Buffer>> response, String requestString )
    {
        if ( response.succeeded() )
        {
            bodyAsString = response.result().bodyAsString();
            int harvesterStatusCode = response.result().statusCode();

            if ( harvesterStatusCode == 200 )
            {
                try
                {
                    Document doc = Xml2Json.XMLStringToXMLDocument( bodyAsString );
                    jsonObject = Xml2Json.recordXml2Json( doc );
                    if ( jsonObject == null )
                    {
                        errorMessage = "Attempted transformation of the Harvester response [" + bodyAsString + "] failed to produce a JSON object";
                        statusCode = 500;
                    }
                    else
                    {
                        statusCode = 200;
                    }
                }
                catch ( IOException | ParserConfigurationException | SAXException e )
                {
                    jsonObject = new JsonObject();
                    errorMessage = "Could not create XML document from Harvester response [" + bodyAsString + "]";
                    statusCode = 500;
                }
            }
            else if ( notFound( harvesterStatusCode, bodyAsString ) )
            {
                statusCode = 404;
                errorMessage = requestString + " not found";
            }
            else
            {
                statusCode = harvesterStatusCode;
                errorMessage = requestString + " encountered error reported as (" + harvesterStatusCode + ") [" + bodyAsString + "] by the harvester";
            }
        }
        else
        {
            statusCode = 500;
            errorMessage = requestString + " failed with " + response.cause().getMessage();
        }
    }

    private boolean notFound( int originalStatusCode, String responseBody )
    {
        return ( originalStatusCode == 500 && responseBody.contains( "NullPointerException" ) && responseBody.contains(
                "Converter.getId(" ) );
    }

}
