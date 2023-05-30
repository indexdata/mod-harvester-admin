package org.folio.harvesteradmin.dataaccess.responsehandlers;

import io.vertx.core.json.JsonObject;

public class ProcessedHarvesterResponseGetUniqueByName extends ProcessedHarvesterResponseGet {

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponseGetUniqueByName(
      JsonObject json, int statusCode, String errorMessage, int records) {
    super(json, statusCode, errorMessage);
    this.recordCount = records;
  }
}
