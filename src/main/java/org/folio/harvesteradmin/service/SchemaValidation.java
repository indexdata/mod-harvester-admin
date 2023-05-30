package org.folio.harvesteradmin.service;

import io.vertx.core.json.JsonObject;

public class SchemaValidation {
  private boolean passed = true;
  private String errorMessage = "";

  /**
   * Performs validations that are not supported by current Vert.X OpenApi validations.
   */
  public SchemaValidation(String endPoint, JsonObject input) {
    if (endPoint.contains("harvestables")) {
      if (input.getString("type").equals("oaiPmh")) {
        if (!input.containsKey("oaiSetName")) {
          passed = false;
          errorMessage +=
              "[Bad Request] Validation[2] error for body application/json: "
                  + "provided object should contain property oaiSetName if type is oaiPmh."
              + System.lineSeparator();
        }
        if (!input.containsKey("metadataPrefix")) {
          passed = false;
          errorMessage += "[Bad Request] Validation[2] error for body application/json: "
              + "provided object should contain property metadataPrefix if type is oaiPmh."
              + System.lineSeparator();
        }
      }
    }
  }

  public static SchemaValidation validateJsonObject(String endPoint, JsonObject json) {
    return new SchemaValidation(endPoint, json);
  }

  public boolean passed() {
    return passed;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

}
