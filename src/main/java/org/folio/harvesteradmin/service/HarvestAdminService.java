package org.folio.harvesteradmin.service;

import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.getIntOrDefault;
import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.pagingPlainText;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.utils.Miscellaneous.getPeriod;
import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;
import static org.folio.okapi.common.HttpResponse.responseText;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.impl.HttpResponseImpl;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.foliodata.ConfigurationsClient;
import org.folio.harvesteradmin.foliodata.SettingsClient;
import org.folio.harvesteradmin.legacydata.JobLauncher;
import org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage;
import org.folio.harvesteradmin.legacydata.responsehandlers.ProcessedHarvesterResponseGet;
import org.folio.harvesteradmin.moduledata.*;
import org.folio.harvesteradmin.moduledata.database.ModuleStorageAccess;
import org.folio.harvesteradmin.moduledata.database.SqlQuery;
import org.folio.harvesteradmin.moduledata.database.Tables;
import org.folio.harvesteradmin.utils.SettableClock;
import org.folio.okapi.common.HttpResponse;
import org.folio.tlib.RouterCreator;
import org.folio.tlib.TenantInitHooks;
import org.folio.tlib.postgres.PgCqlException;
import org.folio.tlib.util.TenantUtil;

/**
 * Main service.
 */
public class HarvestAdminService implements RouterCreator, TenantInitHooks {

  public static final Logger logger = LogManager.getLogger("harvester-admin");

  @Override
  public Future<Router> createRouter(Vertx vertx) {
    return RouterBuilder.create(vertx, "openapi/harvest-admin-1.0.yaml").map(routerBuilder -> {
      handlers(vertx, routerBuilder);
      return routerBuilder.createRouter();
    });
  }

  private void handler(Vertx vertx, RouterBuilder routerBuilder, String operation,
      BiFunction<Vertx, RoutingContext, Future<Void>> method) {

    routerBuilder
        .operation(operation)
        .handler(ctx -> {
          try {
            method.apply(vertx, ctx)
            .onFailure(cause -> exceptionResponse(cause, ctx));
          } catch (Exception e) {  // exception thrown by method
            logger.error("{}: {}", operation, e.getMessage(), e);
            exceptionResponse(e, ctx);
          }
        })
        .failureHandler(this::routerExceptionResponse);  // OpenAPI validation exception
  }

  private void handlers(Vertx vertx, RouterBuilder routerBuilder) {

    handler(vertx, routerBuilder, "getHarvestables", this::getConfigRecords);
    handler(vertx, routerBuilder, "getHarvestable", this::getConfigRecordById);
    handler(vertx, routerBuilder, "postHarvestable", this::postConfigRecord);
    handler(vertx, routerBuilder, "postHarvestableXmlBulk", this::postConfigRecord);
    handler(vertx, routerBuilder, "postHarvestableOaiPmh", this::postConfigRecord);

    handler(vertx, routerBuilder, "putHarvestable", this::putConfigRecord);
    handler(vertx, routerBuilder, "putHarvestableXmlBulk", this::putConfigRecord);
    handler(vertx, routerBuilder, "putHarvestableOaiPmh", this::putConfigRecord);
    handler(vertx, routerBuilder, "deleteHarvestable", this::deleteConfigRecord);

    handler(vertx, routerBuilder, "getJobLog", this::getJobLog);

    handler(vertx, routerBuilder, "getFailedRecords", this::getFailedRecords);

    handler(vertx, routerBuilder, "getFailedRecord", this::getFailedRecord);

    handler(vertx, routerBuilder, "storeJobLog", this::pullJobAndSaveItsLogs);

    handler(vertx, routerBuilder, "storeJobLogWithPostedStatus", this::pullJobAndSaveItsLogs);

    handler(vertx, routerBuilder, "getPreviousJobs", this::getPreviousJobs);
    handler(vertx, routerBuilder, "getPreviousJob", this::getPreviousJobById);
    handler(vertx, routerBuilder, "deletePreviousJob", this::deletePreviousJob);
    handler(vertx, routerBuilder, "postPreviousJob", this::postPreviousJob);
    handler(vertx, routerBuilder, "getPreviousJobLog", this::getPreviousJobLog);
    handler(vertx, routerBuilder, "postPreviousJobLog", this::postPreviousJobLog);
    handler(vertx, routerBuilder, "getFailedRecordsForPreviousJob", this::getFailedRecordsForPreviousJobs);
    handler(vertx, routerBuilder, "postFailedRecords", this::postFailedRecords);
    handler(vertx, routerBuilder, "getFailedRecordForPreviousJob", this::getFailedRecordForPreviousJob);
    handler(vertx, routerBuilder, "getFailedRecordsForPreviousJobs", this::getFailedRecordsForPreviousJobs);

    handler(vertx, routerBuilder, "getStorages", this::getConfigRecords);
    handler(vertx, routerBuilder, "getStorage", this::getConfigRecordById);
    handler(vertx, routerBuilder, "postStorage", this::postConfigRecord);
    handler(vertx, routerBuilder, "putStorage", this::putConfigRecord);
    handler(vertx, routerBuilder, "deleteStorage", this::deleteConfigRecord);

    handler(vertx, routerBuilder, "getTransformations", this::getConfigRecords);
    handler(vertx, routerBuilder, "getTransformation", this::getConfigRecordById);
    handler(vertx, routerBuilder, "postTransformation", this::postConfigRecord);
    handler(vertx, routerBuilder, "putTransformation", this::putConfigRecord);
    handler(vertx, routerBuilder, "deleteTransformation", this::deleteConfigRecord);

    handler(vertx, routerBuilder, "getSteps", this::getConfigRecords);
    handler(vertx, routerBuilder, "getStep", this::getConfigRecordById);
    handler(vertx, routerBuilder, "postStep", this::postConfigRecord);
    handler(vertx, routerBuilder, "putStep", this::putConfigRecord);
    handler(vertx, routerBuilder, "deleteStep", this::deleteConfigRecord);
    handler(vertx, routerBuilder, "getScript", this::getScript);
    handler(vertx, routerBuilder, "putScript", this::putScript);
    handler(vertx, routerBuilder, "getTsas", this::getConfigRecords);
    handler(vertx, routerBuilder, "getTsa", this::getConfigRecordById);
    handler(vertx, routerBuilder, "postTsa", this::postConfigRecord);
    handler(vertx, routerBuilder, "deleteTsa", this::deleteConfigRecord);

    handler(vertx, routerBuilder, "startJob", this::startJob);
    handler(vertx, routerBuilder, "stopJob", this::stopJob);

    handler(vertx, routerBuilder, "purgeAgedLogs", this::purgeAgedLogs);

    handler(vertx, routerBuilder, "getIds", this::generateIds);

  }

  private void exceptionResponse(Throwable cause, RoutingContext routingContext) {
    if (cause.getMessage().toLowerCase().contains("could not find")) {
      HttpResponse.responseError(routingContext, 404, cause.getMessage());
    } else {
      HttpResponse.responseError(routingContext, 400, cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

  }

  /**
   * Returns request validation exception, potentially with improved error message if problem was
   * an error in a polymorph schema, like in `harvestable` of type `oaiPmh` vs `xmlBulk`.
   */
  private void routerExceptionResponse(RoutingContext ctx) {
    String message = ctx.failure().getMessage();
    if (message != null && message.contains("No schema matches")) {
      SchemaValidation validation = SchemaValidation.validateJsonObject(
          ctx.request().path(), ctx.body().asJsonObject());
      if (!validation.passed()) {
        message += ". " + System.lineSeparator() + validation.getErrorMessage();
      }
    }
    HttpResponse.responseError(ctx, ctx.statusCode(), message);
  }

  @Override
  public Future<Void> postInit(Vertx vertx, String tenant, JsonObject tenantAttributes) {
    return new ModuleStorageAccess(vertx, tenant).init(tenantAttributes)
            .onFailure(x -> logger.error("Database initialization failed: " + x.getMessage()))
            .onSuccess(x -> logger.info("Tenant '" + tenant + "' database initialized"));
  }

  private Future<Void> getConfigRecords(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.getConfigRecords(routingContext).map(getResponse -> {
      if (getResponse.wasOK()) {
        responseJson(
            routingContext, 200).end(getResponse.jsonObject().encodePrettily());
      } else {
        responseError(
            routingContext, getResponse.statusCode(), getResponse.errorMessage());
      }
      return null;
    });
  }

  private Future<Void> getConfigRecordById(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    String id = routingContext.request().getParam("id");
    return legacyStorage.getConfigRecordById(routingContext, id).map(getResponse -> {
      if (getResponse.wasOK()) {
        responseJson(
            routingContext, 200)
            .end(getResponse.jsonObject().encodePrettily());
      } else {
        responseError(
            routingContext, getResponse.statusCode(), getResponse.errorMessage());
      }
      return null;
    });
  }

  private Future<Void> postConfigRecord(Vertx vertx, RoutingContext routingContext) {
    SchemaValidation validation = SchemaValidation.validateJsonObject(
        routingContext.request().path(), routingContext.body().asJsonObject());
    if (validation.passed()) {
      String tenant = TenantUtil.tenant(routingContext);
      LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
      return legacyStorage.postConfigRecord(routingContext).map(response -> {
        if (response.wasCreated()) {
          responseJson(
              routingContext, response.statusCode())
              .putHeader("Location", response.location)
              .end(response.jsonObject().encodePrettily());
        } else {
          responseError(routingContext, response.statusCode(), response.errorMessage());
        }
        return null;
      });
    } else {
      responseError(routingContext, 400, validation.getErrorMessage());
      return Future.failedFuture(validation.getErrorMessage());
    }
  }

  private Future<Void> putConfigRecord(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.putConfigRecord(routingContext).map(response -> {
      if (response.wasNoContent()) {
        responseJson(
            routingContext, response.statusCode())
            .end(response.jsonObject().encodePrettily());
      } else {
        responseError(
            routingContext, response.statusCode(), response.errorMessage());
      }
      return null;
    });

  }

  private Future<Void> deleteConfigRecord(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.deleteConfigRecord(routingContext).map(response -> {
      if (response.wasNoContent()) {
        responseJson(
            routingContext, response.statusCode())
            .end(response.jsonObject().encodePrettily());
      } else {
        responseError(
            routingContext, response.statusCode(), response.errorMessage());
      }
      return null;
    });
  }

  private Future<Void> getScript(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.getScript(routingContext)
        .onComplete(response -> {
          if (response.succeeded()) {
            responseText(routingContext, 200).end(response.result());
          } else {
            responseError(routingContext, 400, response.cause().getMessage());
          }
        }).mapEmpty();
  }

  private Future<Void> putScript(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.putScript(routingContext)
        .onSuccess(response -> {
          if (response.statusCode() == 204) {
            responseText(routingContext, 204).end();
          } else {
            responseError(routingContext, response.statusCode(), response.errorMessage());
          }
        })
        .onFailure(response -> responseError(routingContext, 500, response.getMessage()))
        .mapEmpty();
  }

  private Future<Void> startJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    JobLauncher launcher = new JobLauncher(vertx, tenant);
    launcher.startJob(routingContext);
    return Future.succeededFuture();
  }

  private Future<Void> stopJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    JobLauncher launcher = new JobLauncher(vertx, tenant);
    launcher.stopJob(routingContext);
    return Future.succeededFuture();
  }

  private Future<Void> purgeAgedLogs(Vertx vertx, RoutingContext routingContext) {
    logger.info("Running timer process: purge aged logs");
    final String SETTINGS_SCOPE = "mod-harvester-admin";
    final String SETTINGS_KEY = "PURGE_LOGS_AFTER";
    SettingsClient.getStringValue(routingContext,
                    SETTINGS_SCOPE,
                    SETTINGS_KEY)
                    .onComplete(settingsValue -> {
                      if (settingsValue.result() != null) {
                        applyPurgeOfPastJobs(vertx, routingContext, settingsValue.result());
                      } else {
                        final String CONFIGS_MODULE = "mod-harvester-admin";
                        final String CONFIGS_CONFIG_NAME = "PURGE_LOGS_AFTER";
                        ConfigurationsClient.getStringValue(routingContext,
                                        CONFIGS_MODULE,
                                        CONFIGS_CONFIG_NAME)
                                .onComplete(configsValue -> applyPurgeOfPastJobs(vertx, routingContext, configsValue.result()));
                      }
                    });
    return Future.succeededFuture();
  }

  private void applyPurgeOfPastJobs(Vertx vertx, RoutingContext routingContext, String purgeSetting) {
    Period ageForDeletion = getPeriod(purgeSetting,3, "MONTHS");
    LocalDateTime untilDate = SettableClock.getLocalDateTime().minus(ageForDeletion).truncatedTo(ChronoUnit.MINUTES);
    logger.info("Running timer process: purging aged logs from before " + untilDate);
    String tenant = TenantUtil.tenant(routingContext);
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
    moduleStorage.purgePreviousJobsByAge(untilDate)
            .onSuccess(x -> routingContext.response().setStatusCode(204).end())
            .onFailure(e -> routingContext.response().setStatusCode(500).end(e.getMessage()));
  }

  private Future<Void> getJobLog(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.getJobLog(routingContext)
        .onComplete(response -> {
          if (response.succeeded()) {
            int offset = getIntOrDefault(routingContext.request().getParam("offset"), 0);
            int limit = getIntOrDefault(routingContext.request().getParam("limit"), 100000);
            String log = pagingPlainText(response.result().bodyAsString(), offset, limit);
            responseText(
                routingContext,
                response.result().statusCode())
                .end(log == null ? "No logs found for this job." : log);
          }
        })
        .mapEmpty();
  }

  private Future<Void> getFailedRecords(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.getFailedRecords(routingContext).onComplete(getResponse -> {
      if (getResponse.result().wasOK()) {
        responseJson(
            routingContext, 200).end(getResponse.result().jsonObject().encodePrettily());
      } else {
        responseError(
            routingContext, getResponse.result().statusCode(), getResponse.result().errorMessage());
      }
    }).mapEmpty();
  }

  private Future<Void> getFailedRecord(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.getFailedRecord(routingContext).onComplete((getResponse -> {
      if (getResponse.result().wasOK()) {
        responseJson(
            routingContext, 200).end(getResponse.result().jsonObject().encodePrettily());
      } else {
        responseError(
            routingContext, getResponse.result().statusCode(), getResponse.result().errorMessage());
      }
    })).mapEmpty();
  }

  private Future<Void> pullJobAndSaveItsLogs(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    String harvestableId = routingContext.request().getParam("id");
    return legacyStorage.getConfigRecordById(HARVESTER_HARVESTABLES_PATH, harvestableId)
        .onComplete(harvestable -> {
          if (harvestable.result().wasNotFound()) {
            responseText(routingContext, 404).end("No job found with ID " + harvestableId);
          } else if (harvestable.result().wasOK()) {
            final JsonObject jobStatus =
                (routingContext.body() != null && routingContext.body().asJsonObject() != null
                    ? routingContext.body().asJsonObject() : new JsonObject());
            String harvestStartedDate =
                jobStatus.isEmpty()
                    || jobStatus.getString(HarvestJobField.STARTED.propertyName()) == null
                ? harvestable.result().jsonObject().getString("lastHarvestStarted")
                : jobStatus.getString(HarvestJobField.STARTED.propertyName());
            logger.info("Looking for logs by start date: " + harvestStartedDate);
            CompositeFuture.all(
                    legacyStorage.getJobLog(harvestableId, harvestStartedDate),
                    legacyStorage.getFailedRecords(harvestableId, 0, 10000))
                .onComplete(logResults -> {
                  HttpResponseImpl<Buffer> logsResponse = logResults.result().resultAt(0);
                  ProcessedHarvesterResponseGet failuresResponse = logResults.result().resultAt(1);
                  ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
                  HarvestJob job =
                      HarvestJob.fromHarvestableJson(harvestable.result().jsonObject());
                  List<StoredEntity> failedRecords =  failedRecordsFromLegacyHarvesterJson(job.getId(),
                          failuresResponse.jsonObject().getJsonArray("failedRecords"));
                  if (!jobStatus.isEmpty()) {
                    // Job status was included in request, overwrite pulled properties
                    job.setFinished(jobStatus.getString(HarvestJobField.FINISHED.propertyName()));
                    if (jobStatus.containsKey(HarvestJobField.AMOUNT_HARVESTED.propertyName())) {
                      job.setAmountHarvested(
                          jobStatus.getString(HarvestJobField.AMOUNT_HARVESTED.propertyName()));
                    }
                    job.setMessage(jobStatus.getString(HarvestJobField.MESSAGE.propertyName()));
                    job.setStarted(harvestStartedDate);
                    job.setStatus(jobStatus.getString(HarvestJobField.STATUS.propertyName()));
                  }
                  moduleStorage.storeHarvestJob(job)
                          .onComplete(jobStored -> CompositeFuture.all(
                              moduleStorage.storeLogStatements(job.getId(), logsResponse.bodyAsString()),
                              moduleStorage.storeFailedRecords(job.getId(),
                                  failedRecords)
                          ).onComplete(
                              result -> {
                                if (result.succeeded()) {
                                  responseText(routingContext, 200)
                                      .end("Saved job with logs and record failures if any.");
                                } else {
                                  responseError(routingContext, 500,
                                      "There was an error saving the job or it's logs: "
                                          + result.cause().getMessage());
                                }
                              }
                          ));
                });
          } else {
            responseError(routingContext,
                harvestable.result().statusCode(), harvestable.cause().getMessage());
          }
        })
        .mapEmpty();
  }
  private static List<StoredEntity> failedRecordsFromLegacyHarvesterJson(UUID harvestJobId, JsonArray failedRecords) {
    List<StoredEntity> list = new ArrayList<>();
    for (Object rec : failedRecords) {
      JsonObject failedRecord = (JsonObject) rec;
      list.add(RecordFailure.fromLegacyHarvesterJson(harvestJobId, failedRecord));
    }
    return list;
  }

  private static List<StoredEntity> failedRecordsFromHarvesterAdminJson(UUID harvestJobId, JsonArray failedRecords) {
    List<StoredEntity> list = new ArrayList<>();
    for (Object rec : failedRecords) {
      JsonObject failedRecord = (JsonObject) rec;
      list.add(RecordFailure.fromHarvesterAdminJson(harvestJobId, failedRecord));
    }
    return list;
  }

  private Future<Void> getPreviousJobs(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);

    String fromDateTime = routingContext.request().getParam("from");
    String untilDateTime = routingContext.request().getParam("until");
    String timeRange = null;
    if (fromDateTime != null && untilDateTime != null) {
      timeRange = " (finished >= '" + fromDateTime + "'  AND finished <= '" + untilDateTime + "') ";
    } else if (fromDateTime != null) {
      timeRange = " finished >= '" + fromDateTime + "' ";
    } else if (untilDateTime != null) {
      timeRange = " finished <= '" + untilDateTime + "' ";
    }

    SqlQuery query;
    try {
      query = HarvestJob.entity()
          .makeSqlFromCqlQuery(routingContext, moduleStorage.schemaDotTable(Tables.harvest_job))
          .withAdditionalWhereClause(timeRange);
    } catch (PgCqlException pce) {
      responseText(routingContext, 400)
          .end("Could not execute query to retrieve jobs: " + pce.getMessage() + " Request:" + routingContext.request().absoluteURI());
      return Future.succeededFuture();
    } catch (Exception e) {
      logger.error("getPreviousJobs from={} until={}: {}", fromDateTime, untilDateTime, e.getMessage(), e);
      return Future.failedFuture(e.getMessage());
    }
    JsonObject responseJson = new JsonObject();
    JsonArray previousJobs = new JsonArray();
    responseJson.put("previousJobs", previousJobs);
    return moduleStorage.getPreviousJobs(query.getQueryWithLimits())
        .compose(jobs -> {
          for (HarvestJob job : jobs) {
            previousJobs.add(job.asJson());
          }
          return moduleStorage.getCount(query.getCountingSql());
        })
        .onSuccess(count -> {
          responseJson.put("totalRecords", count);
          responseJson(routingContext, 200).end(responseJson.encodePrettily());
        })
        .onFailure(e -> {
          logger.error("getPreviousJobs from={} until={}: {}", fromDateTime, untilDateTime, e.getMessage(), e);
          responseText(routingContext, 500)
              .end("Problem retrieving jobs: " + e.getMessage());
        })
        .mapEmpty();
  }

  private Future<Void> postPreviousJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
    HarvestJob job =
            HarvestJob.fromHarvestJobJson(routingContext.body().asJsonObject());
    return moduleStorage.storeHarvestJob(job)
            .onComplete(harvestJobId -> {
              if (harvestJobId.result() == null) {
                responseText(routingContext, 400).end("Failed to insert harvest job run: " + harvestJobId.cause().getMessage());
              } else {
                responseJson(routingContext, 201).end(job.asJson().encodePrettily());
              }
            }).mapEmpty();
  }

  private Future<Void> getPreviousJobById(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
    return moduleStorage.getPreviousJobById(id)
        .onComplete(harvestJob -> {
          if (harvestJob.result() == null) {
            responseText(routingContext, 404).end("Found no job with id " + id);
          } else {
            responseJson(routingContext, 200).end(harvestJob.result().asJson().encodePrettily());
          }
        }).mapEmpty();
  }

  private Future<Void> deletePreviousJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
    return moduleStorage.deletePreviousJob(id)
        .onComplete(deleted -> {
          if (deleted.succeeded()) {
            responseText(routingContext, 200).end("Job " + id + " and its logs deleted.");
          } else {
            String message = deleted.cause().getMessage();
            responseError(
                routingContext, message.startsWith("No job history found") ? 404 : 500, message);
          }
        });
  }

  private Future<Void> getPreviousJobLog(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    String contentType = routingContext.request().getHeader("Accept");
    logger.info("Request for previous logs in " + contentType + ".");
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    String fromDateTime = routingContext.request().getParam("from");
    String untilDateTime = routingContext.request().getParam("until");
    String timeRange = null;
    if (fromDateTime != null && untilDateTime != null) {
      timeRange = "time_stamp >= '" + fromDateTime + "' AND time_stamp <= '" + untilDateTime + "'";
    } else if (fromDateTime != null) {
      timeRange = "time_stamp >= '" + fromDateTime + "'";
    } else if (untilDateTime != null) {
      timeRange = "time_stamp <= '" + untilDateTime + "'";
    }

    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
    SqlQuery queryFromCql = LogLine.entity()
        .makeSqlFromCqlQuery(routingContext, moduleStorage.schemaDotTable(Tables.log_statement))
        .withAdditionalWhereClause(timeRange);
    if (contentType != null && contentType.contains("json")) {
      return moduleStorage.getLogsAsJsonForPreviousJob(id, queryFromCql)
          .onComplete(jobLog -> {
            if (jobLog.succeeded()) {
              if (jobLog.result().isEmpty()) {
                moduleStorage.getPreviousJobById(id).onComplete(harvestJob -> {
                  if (harvestJob.result() == null) {
                    responseText(routingContext, 404)
                        .end("Found no previous job with ID " + id);
                  } else {
                    responseText(routingContext, 200)
                        .end("Previous job with ID " + id + ", "
                            + harvestJob.result().getName() + ", has no logs.");
                  }
                });
              } else {
                responseJson(routingContext, 200).end(jobLog.result().encodePrettily());
              }
            } else {
              responseError(routingContext, 500, jobLog.cause().getMessage());
            }
          }).mapEmpty();
    } else {
      return moduleStorage.getLogsForPreviousJob(id, queryFromCql)
          .onComplete(jobLog -> {
            if (jobLog.succeeded()) {
              if (jobLog.result().isEmpty()) {
                moduleStorage.getPreviousJobById(id).onComplete(harvestJob -> {
                  if (harvestJob.result() == null) {
                    responseText(routingContext, 404)
                        .end("Found no previous job with ID " + id);
                  } else {
                    responseText(routingContext, 200)
                        .end("Previous job with ID " + id + ", "
                            + harvestJob.result().getName() + ", has no logs.");
                  }
                });
              } else {
                responseText(routingContext, 200).end(jobLog.result());
              }
            } else {
              responseError(routingContext, 500, jobLog.cause().getMessage());
            }
          }).mapEmpty();
    }
  }

  public Future<Void> postPreviousJobLog (Vertx vertx, RoutingContext routingContext) {
    String log = routingContext.body().asString();
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
    return moduleStorage.storeLogStatements(id, log)
            .onComplete(failedRecordsResponse -> {
      if (failedRecordsResponse.succeeded()) {
        responseText(routingContext, 201).end("Job logs saved for job " + id);
      } else {
        responseText(routingContext, 500).end("Could not store the logs for job  " + id
                + failedRecordsResponse.cause().getMessage());
      }
    }).mapEmpty();
  }

  private Future<Void> getFailedRecordsForPreviousJobs(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);

    SqlQuery queryFromCql = RecordFailure.entity().makeSqlFromCqlQuery(
        routingContext, moduleStorage.schemaDotTable(Tables.record_failure_view))
        .withDefaultLimit("100");
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    RequestParameter jobId = params.pathParameter("id");
    RequestParameter from = params.queryParameter("from");
    RequestParameter until = params.queryParameter("until");

    String timeRange = null;
    if (from != null && until != null) {
      timeRange = " (time_stamp >= '" + from.getString()
          + "'  AND time_stamp <= '" + until.getString() + "') ";
    } else if (from != null) {
      timeRange = " time_stamp >= '" + from.getString() + "' ";
    } else if (until != null) {
      timeRange = " time_stamp <= '" + until.getString() + "' ";
    }

    if (jobId != null) {
      queryFromCql.withAdditionalWhereClause("harvest_job_id = '" + jobId + "'");
    }
    if (timeRange != null) {
      queryFromCql.withAdditionalWhereClause(timeRange);
    }

    return moduleStorage.getFailedRecordsForPreviousJobs(queryFromCql).onComplete(
      failuresList -> {
        if (failuresList.succeeded()) {
          JsonObject responseJson = new JsonObject();
          JsonArray recordFailures = new JsonArray();
          responseJson.put("failedRecords", recordFailures);
          List<RecordFailure> failures = failuresList.result();
          for (RecordFailure failure : failures) {
            recordFailures.add(failure.asJson());
          }
          moduleStorage.getCount(queryFromCql.getCountingSql()).onComplete(
              count -> {
                responseJson.put("totalRecords", count.result());
                responseJson(routingContext, 200).end(responseJson.encodePrettily());
              }
          );
        }
      }
    ).mapEmpty();
  }

  private Future<Void> getFailedRecordForPreviousJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
    return moduleStorage.getFailedRecordForPreviousJob(id).onComplete(
        failureRecord -> {
          if (failureRecord.succeeded()) {
            RecordFailure failure = failureRecord.result();
            responseJson(routingContext, 200).end(failure.asJson().encodePrettily());
          } else {
            if (failureRecord.cause().getMessage().startsWith("No failed record")) {
              responseText(routingContext, 404)
                  .end(failureRecord.cause().getMessage());
            }
            responseText(routingContext, 500)
                .end("Problem retrieving jobs: " + failureRecord.cause().getMessage());
          }
        }
    ).mapEmpty();
  }

  private Future<Void> postFailedRecords(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    JsonObject failedRecordsJson = routingContext.body().asJsonObject();
    List<StoredEntity> failedRecords =
            failedRecordsFromHarvesterAdminJson(id, failedRecordsJson.getJsonArray("failedRecords"));
    return moduleStorage.storeFailedRecords(id, failedRecords)
            .onComplete(failedRecordsResponse -> {
              if (failedRecordsResponse.succeeded()) {
                responseText(routingContext, 201).end("Failed records inserted.");
              } else {
                responseText(routingContext, 500).end("Could not insert failed records "
                        + failedRecordsResponse.cause().getMessage());
              }
            }).mapEmpty();
  }


  private Future<Void> generateIds(Vertx vertx, RoutingContext routingContext) {
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    int count = 1;
    if (params.queryParameter("count") != null) {
      count = Math.min(params.queryParameter("count").getInteger(), 100);
    }
    StringBuilder response = new StringBuilder();
    for (int i = 0; i < count; i++) {
      response
          .append(LegacyHarvesterStorage.getRandomFifteenDigitString())
          .append(System.lineSeparator());
    }
    responseText(routingContext, 200).end(response.toString());
    return Future.succeededFuture();
  }
}
