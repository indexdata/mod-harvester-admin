package org.folio.harvesteradmin.legacydata.responsehandlers;

import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.NO_CONTENT;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class ProcessedHarvesterResponseDelete extends ProcessedHarvesterResponse {

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponseDelete(AsyncResult<HttpResponse<Buffer>> response,
                                          String requestPath, String legacyPath) {

    statusCode = response.result().statusCode();
    if (statusCode != NO_CONTENT) {
      this.errorMessage = response.result().statusMessage();
    }

  }

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponseDelete(int statusCode, String errorMessage) {
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
    this.bodyAsString = this.bodyAsString == null ? "no response body" : this.bodyAsString;
  }

}

