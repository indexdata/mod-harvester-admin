package org.folio.harvesteradmin.dataaccess.responsehandlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.dataaccess.dataconverters.HarvesterXml2Json;
import org.folio.harvesteradmin.dataaccess.statics.LegacyServiceConfig;


public class ProcessedHarvesterResponseGetById extends ProcessedHarvesterResponse {
  protected static final Logger logger =
      LogManager.getLogger(ProcessedHarvesterResponseGetById.class);

  /**
   * Constructor.
   */
  public ProcessedHarvesterResponseGetById(AsyncResult<HttpResponse<Buffer>> response,
                                           String apiPath, String id, String tenant) {
    if (response.succeeded()) {
      bodyAsString = response.result().bodyAsString();
      int harvesterStatusCode = response.result().statusCode();
      if (harvesterStatusCode == 200) {
        logger.debug("Retrieved a record from Harvester " + bodyAsString);
        JsonObject transformed = HarvesterXml2Json.convertRecordToJson(bodyAsString);
        if (transformed == null) {
          errorMessage = "Attempted transformation of the Harvester response [" + bodyAsString
              + "] failed to produce a JSON object";
          statusCode = 500;
        } else if (LegacyServiceConfig.filterByTenant
            && !apiPath.contains("failed-records") // got no acl, but filter enforced via the job
            && (!tenant.equals((apiPath.contains("tsas") // tsas got no acl, check step's instead
            ? transformed.getJsonObject("step").getString("acl")
            : transformed.getString("acl"))))) {
          statusCode = 404;
          errorMessage = apiPath + "/" + id + " not found";
        } else {
          jsonObject = transformed;
          statusCode = 200;
        }
      } else if (notFound(harvesterStatusCode, bodyAsString)) {
        statusCode = 404;
        errorMessage = apiPath + "/" + id + " not found";
      } else {
        statusCode = harvesterStatusCode;
        errorMessage =
            apiPath + "/" + id + " encountered error reported as (" + harvesterStatusCode + ") ["
                + bodyAsString + "] by the harvester";
      }
    } else {
      statusCode = 500;
      errorMessage =
          "GET request to " + apiPath + "/" + id + " failed with: " + response.cause().getMessage();
    }
  }

  private boolean notFound(int originalStatusCode, String responseBody) {
    return (originalStatusCode == 500 && responseBody.contains("NullPointerException")
        && responseBody.contains("Converter.getId("));
  }

  /**
   * Is record found.
   */
  public boolean found() {
    return (statusCode == 200);
  }

}
