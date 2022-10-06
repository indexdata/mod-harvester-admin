package org.folio.harvesteradmin.dataaccess.responsehandlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;

public class ProcessedHarvesterResponsePost extends ProcessedHarvesterResponse {

  public String location;

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponsePost(
      AsyncResult<HttpResponse<Buffer>> response,
      String requestPath,
      String legacyApiPath,
      ProcessedHarvesterResponseGetById confirmationResponse) {
    // Check that POST went well and subsequent GET successfully requested the created entity.
    if (response.succeeded()) {
      if (response.result().statusCode() == 201
          && response.result().getHeader("Location") != null) {
        if (confirmationResponse.statusCode == 200) {
          statusCode = 201;
          jsonObject = confirmationResponse.jsonObject;
          location = requestPath + "/" + jsonObject.getString("id");
        } else {
          statusCode = 500;
          errorMessage = "There was a problem confirming that entity POSTed to " + legacyApiPath
              + " was persisted, confirmation request status " + confirmationResponse.statusCode
              + ", message: " + confirmationResponse.errorMessage;
        }
      } else {
        statusCode = 500;
        errorMessage =
            "Error: Could not obtain location from Harvester response; needed to confirm that "
                + "POSTed record was persisted: ("
                + response.result().statusCode() + ") " + response.result().bodyAsString();
      }
    } else {
      statusCode = 500;
      errorMessage =
          "POST request to " + legacyApiPath + " failed with: " + response.cause().getMessage();
    }
  }

  public ProcessedHarvesterResponsePost setConfirmationResponse(JsonObject json) {
    jsonObject = json;
    return this;
  }

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponsePost(int statusCode, String errorMessage) {
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
    this.bodyAsString = this.bodyAsString == null ? "no response body" : this.bodyAsString;
  }


}
