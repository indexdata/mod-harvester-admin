package org.folio.harvesteradmin.legacydata;

import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.okapi.common.HttpResponse.responseJson;
import static org.folio.okapi.common.HttpResponse.responseText;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class JobLauncher extends LegacyHarvesterStorage {
  private static final String PROP_NAME = "name";
  private static final String PROP_HARVEST_IMMEDIATELY = "harvestImmediately";
  private static final String PROP_LAST_UPDATED = "lastUpdated";
  private static final String PROP_HARVESTABLE_ID = "harvestableId";
  private static final String PROP_INITIATED = "initiated";
  private static final String TRUE = "true";

  private static final int BAD_REQUEST = 400;
  private static final int OK = 200;

  private final SimpleDateFormat dateFormat;

  /**
   * Constructor.
   */
  public JobLauncher(Vertx vertx, String tenant) {
    super(vertx, tenant);

    dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /**
   * Starts a harvest job.
   */
  public void startJob(RoutingContext routingContext) {

    String harvestableId = routingContext.request().getParam("id");
    getConfigRecordById(HARVESTER_HARVESTABLES_PATH, harvestableId).onComplete(lookUp -> {
      if (lookUp.succeeded()) {
        if (lookUp.result().wasNotFound()) {
          responseText(routingContext, NOT_FOUND).end(
              "Did not find a harvest configuration with ID " + harvestableId
                  + ". No job started.");
        } else if (lookUp.result().wasOK() && lookUp.result().jsonObject()
            .getString("currentStatus").equals("RUNNING")) {
          responseText(routingContext, BAD_REQUEST).end(
              "A job with this configuration is already running " + lookUp.result().jsonObject()
                  .getString("name"));
        } else if (lookUp.result().wasOK()) {
          JsonObject harvestConfig = lookUp.result().jsonObject().copy();
          harvestConfig.put(PROP_HARVEST_IMMEDIATELY, TRUE);
          harvestConfig.put(PROP_LAST_UPDATED, dateFormat.format(new Date()));
          putConfigRecord(routingContext, HARVESTER_HARVESTABLES_PATH, harvestConfig,
              harvestableId).onComplete(putResponse -> {
                if (putResponse.succeeded()) {
                  JsonObject responseOk = new JsonObject();
                  responseOk.put(PROP_HARVESTABLE_ID, harvestableId);
                  responseOk.put(PROP_NAME, harvestConfig.getString("name"));
                  responseOk.put(PROP_INITIATED, harvestConfig.getString(PROP_LAST_UPDATED));
                  responseJson(routingContext, OK).end(responseOk.encodePrettily());
                } else {
                  responseText(routingContext, INTERNAL_SERVER_ERROR).end(
                      "An error occurred when trying to start job  " + harvestableId + ": "
                          + putResponse.cause().getMessage());
                }
              });
        } else {
          responseText(routingContext, lookUp.result().statusCode()).end(
              "A problem occurred when looking for the job to start:" + lookUp.result()
                  .errorMessage());
        }
      } else {
        responseText(routingContext, INTERNAL_SERVER_ERROR).end(
            "Could not look up harvest configuration. Job not started.");
      }
    });
  }

  /**
   * Stops a harvest job.
   */
  public void stopJob(RoutingContext routingContext) {
    String harvestableId = routingContext.request().getParam("id");
    getConfigRecordById(HARVESTER_HARVESTABLES_PATH, harvestableId).onComplete(lookUp -> {
      if (lookUp.succeeded()) {
        if (lookUp.result().wasNotFound()) {
          responseText(routingContext, NOT_FOUND).end(
              "Did not find a harvest configuration with ID " + harvestableId
                  + ". No job stopped.");
        } else if (lookUp.result().wasOK() && !lookUp.result().jsonObject()
            .getString("currentStatus").equals("RUNNING")) {
          responseText(routingContext, BAD_REQUEST).end(
              "This configuration has no running job, cannot stop it. " + lookUp.result()
                  .jsonObject().getString("name"));
        } else if (lookUp.result().wasOK()) {
          JsonObject harvestConfig = lookUp.result().jsonObject().copy();
          harvestConfig.put(PROP_LAST_UPDATED, dateFormat.format(new Date()));
          putConfigRecord(routingContext, HARVESTER_HARVESTABLES_PATH, harvestConfig,
              harvestableId).onComplete(putResponse -> {
                if (putResponse.succeeded()) {
                  JsonObject responseOk = new JsonObject();
                  responseOk.put(PROP_HARVESTABLE_ID, harvestableId);
                  responseOk.put(PROP_NAME, harvestConfig.getString("name"));
                  responseOk.put(PROP_INITIATED, harvestConfig.getString(PROP_LAST_UPDATED));
                  responseJson(routingContext, OK).end(responseOk.encodePrettily());
                } else {
                  responseText(routingContext, INTERNAL_SERVER_ERROR).end(
                      "An error occurred when trying to stop job using " + harvestableId + ": "
                          + putResponse.cause().getMessage());
                }
              });
        } else {
          responseText(routingContext, lookUp.result().statusCode()).end(
              "A problem occurred when looking for the job to stop:" + lookUp.result()
                  .errorMessage());
        }
      } else {
        responseText(routingContext, INTERNAL_SERVER_ERROR).end(
            "Could not look up harvest configuration. No job stopped.");
      }
    });

  }

}