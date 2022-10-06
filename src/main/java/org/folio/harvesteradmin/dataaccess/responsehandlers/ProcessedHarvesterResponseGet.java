package org.folio.harvesteradmin.dataaccess.responsehandlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.folio.harvesteradmin.dataaccess.LegacyHarvesterStorage;
import org.folio.harvesteradmin.dataaccess.dataconverters.HarvesterXml2Json;

public class ProcessedHarvesterResponseGet extends ProcessedHarvesterResponse {
  private int totalRecords;
  private static final Pattern exceptionDescriptionPattern =
      Pattern.compile("(Exception Description:.*?[\\n\\r]+.*?[\\n\\r]+)", Pattern.DOTALL);

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
        totalRecords = Integer.parseInt(jsonObject.getString("totalRecords"));
      } else if (harvesterStatusCode == 500
          && bodyAsString.contains(
              "An exception occurred while creating a query in EntityManager")) {
        Matcher m = exceptionDescriptionPattern.matcher(bodyAsString);
        if (m.find()) {
          errorMessage = "Query failed: " + m.group(1).replaceAll("&#39;", "'");
        } else {
          errorMessage = "Query failed: " + bodyAsString;
        }
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
   * Gets total records.
   */
  public int totalRecords() {
    return totalRecords;
  }

  /**
   * Gets records.
   */
  public List getRecords() {
    return jsonObject.getJsonArray(LegacyHarvesterStorage.mapToNameOfRootOfResultSet(harvesterPath))
        .getList();
  }
}
