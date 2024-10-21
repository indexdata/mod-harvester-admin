package org.folio.harvesteradmin.legacydata.responsehandlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class ProcessedHarvesterResponsePut extends ProcessedHarvesterResponse {
  public AsyncResult<HttpResponse<Buffer>> harvesterResponse;

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponsePut(
      AsyncResult<HttpResponse<Buffer>> harvesterResponse,
      String requestPath,
      String legacyPath) {
    this.harvesterResponse = harvesterResponse;
    this.bodyAsString = harvesterResponse.result().bodyAsString();
    statusCode = harvesterResponse.result().statusCode();
  }

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponsePut(int statusCode, String errorMessage) {
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
    this.bodyAsString = this.bodyAsString == null ? "no response body" : this.bodyAsString;
  }

}
