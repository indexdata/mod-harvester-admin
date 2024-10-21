package org.folio.harvesteradmin.legacydata.responsehandlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.folio.harvesteradmin.legacydata.dataconverters.HarvesterXml2Json;

public class ProcessedHarvesterResponseGet extends ProcessedHarvesterResponse {

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponseGet(AsyncResult<HttpResponse<Buffer>> response,
                                       String harvesterPath, String query) {
    this.harvesterPath = harvesterPath;
    if (response.succeeded()) {
      bodyAsString = response.result().bodyAsString();
      int harvesterStatusCode = response.result().statusCode();
      statusCode = harvesterStatusCode;
      if (harvesterStatusCode == 200) {
        jsonObject = HarvesterXml2Json.convertRecordSetToJson(bodyAsString);
      } else if (harvesterStatusCode == 500
          && bodyAsString.contains(
              "An exception occurred while creating a query in EntityManager")) {
        errorMessage = "Query failed: " + bodyAsString;
        statusCode = 400;
        jsonObject = new JsonObject();
      } else {
        errorMessage =
            "GET request to " + harvesterPath + (query != null ? " for query " + query : "")
                + " did not return OK (200) but " + statusCode + ": " + bodyAsString;
        jsonObject = new JsonObject();
      }
    } else {
      statusCode = 500;
      errorMessage = "GET request to " + harvesterPath + " failed"
          + (query != null ? " for query " + query : "") + ". Cause: "
          + response.cause().getMessage();
    }
  }

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponseGet(JsonObject json, int statusCode, String errorMessage) {
    this.jsonObject = json;
    this.statusCode = statusCode;
    if (errorMessage != null && ! errorMessage.isEmpty()) {
      this.errorMessage = errorMessage;
    }
  }

}
