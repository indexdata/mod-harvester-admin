package org.folio.harvesteradmin.service;

import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.getIntOrDefault;
import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.pagingPlainText;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.utils.Miscellaneous.getPeriod;
import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;
import static org.folio.okapi.common.HttpResponse.responseText;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.impl.HttpResponseImpl;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import io.vertx.ext.web.openapi.router.RouterBuilder;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import io.vertx.openapi.contract.OpenAPIContract;
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
    return OpenAPIContract.from(vertx, "openapi/harvest-admin-1.0.yaml")
        .map(contract -> {
          RouterBuilder routerBuilder = RouterBuilder.create(vertx, contract);
          handlers(vertx, routerBuilder);
          return routerBuilder.createRouter();
        }).onSuccess(res -> logger.info("OpenAPI parsed OK"));
  }

  private void handler(Vertx vertx, RouterBuilder routerBuilder, String operation,
                       BiFunction<Vertx, RoutingContext, Future<Void>> method) {
    routerBuilder.getRoute(operation)
        .addHandler(ctx -> {
          try {
            method.apply(vertx, ctx)
                .onFailure(cause -> {
                  logger.error("Handler failure {}: {}", operation, cause.getMessage());
                  exceptionResponse(cause, ctx);
                });
          } catch (Exception e) {  // exception thrown by method
            logger.error("Handler exception {}: {}", operation, e.getMessage(), e);
            exceptionResponse(e, ctx);
          }
        })
        .addFailureHandler(this::routerExceptionResponse);  // OpenAPI validation exception
  }

  /**
   * For POSTing text, PUTting xml
   */
  private void nonValidatingHandler(Vertx vertx, RouterBuilder routerBuilder, String operation,
                                    BiFunction<Vertx, RoutingContext, Future<Void>> method) {
    routerBuilder.getRoute(operation)
        .addHandler(new BodyHandlerImpl())
        .setDoValidation(false)
        .addHandler(ctx -> {
          try {
            method.apply(vertx, ctx)
                .onFailure(cause -> {
                  logger.error("Non-validating handler failure {}: {}", operation, cause.getMessage());
                  exceptionResponse(cause, ctx);
                });
          } catch (Exception e) {  // exception thrown by method
            logger.error("Non-validating handler exception {}: {}", operation, e.getMessage(), e);
            exceptionResponse(e, ctx);
          }
        })
        .addFailureHandler(this::routerExceptionResponse);  // OpenAPI validation exception
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
    nonValidatingHandler(vertx, routerBuilder, "postPreviousJobLog", this::postPreviousJobLog);
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
    nonValidatingHandler(vertx, routerBuilder, "putScript", this::putScript);
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
    logger.error("Router exception: " + message + Arrays.toString(ctx.failure().getStackTrace()));
    HttpResponse.responseError(ctx, ctx.statusCode(), message + Arrays.toString(ctx.failure().getStackTrace()));
  }

  @Override
  public Future<Void> postInit(Vertx vertx, String tenant, JsonObject tenantAttributes) {
    return new ModuleStorageAccess(vertx, tenant).init(tenantAttributes)
        .onFailure(x -> logger.error("Database initialization failed: " + x.getMessage()))
        .onSuccess(x -> logger.info("Tenant '" + tenant + "' database initialized"));
  }

  private Future<Void> getConfigRecords(Vertx vertx, RoutingContext routingContext) {
    AdminRequest request = new RequestValidated(vertx, routingContext);
    return request
        .legacyHarvesterAccess()
        .getConfigRecords(request)
        .map(getResponse -> {
          if (getResponse.wasOK()) {
            responseJson(routingContext, 200).end(getResponse.jsonObject().encodePrettily());
          } else {
            responseError(routingContext, getResponse.statusCode(), getResponse.errorMessage());
          }
          return null;
        });
  }

  private Future<Void> getConfigRecordById(Vertx vertx, RoutingContext routingContext) {
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    String id = adminRequest.requestParam("id");
    return adminRequest
        .legacyHarvesterAccess()
        .getConfigRecordById(adminRequest.harvesterPathFromRequestPath(), id)
        .map(getResponse -> {
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    SchemaValidation validation = SchemaValidation.validateJsonObject(
        adminRequest.path(), adminRequest.bodyAsJson());
    if (validation.passed()) {
      return adminRequest
          .legacyHarvesterAccess()
          .postConfigRecord(adminRequest)
          .map(response -> {
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    return adminRequest.legacyHarvesterAccess().putConfigRecord(adminRequest).map(response -> {
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    return adminRequest.legacyHarvesterAccess().deleteConfigRecord(routingContext).map(response -> {
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    return adminRequest.legacyHarvesterAccess().getScript(routingContext)
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    JobLauncher launcher = new JobLauncher(adminRequest);
    launcher.startJob(adminRequest);
    return Future.succeededFuture();
  }

  private Future<Void> stopJob(Vertx vertx, RoutingContext routingContext) {
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    JobLauncher launcher = new JobLauncher(adminRequest);
    launcher.stopJob(adminRequest);
    return Future.succeededFuture();
  }

  private Future<Void> purgeAgedLogs(Vertx vertx, RoutingContext routingContext) {
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    logger.info("Running timer process: purge aged logs");
    final String SETTINGS_SCOPE = "mod-harvester-admin";
    final String SETTINGS_KEY = "PURGE_LOGS_AFTER";
    SettingsClient.getStringValue(routingContext,
            SETTINGS_SCOPE,
            SETTINGS_KEY)
        .onComplete(settingsValue -> {
          if (settingsValue.result() != null) {
            applyPurgeOfPastJobs(adminRequest, settingsValue.result());
          } else {
            final String CONFIGS_MODULE = "mod-harvester-admin";
            final String CONFIGS_CONFIG_NAME = "PURGE_LOGS_AFTER";
            ConfigurationsClient.getStringValue(routingContext,
                    CONFIGS_MODULE,
                    CONFIGS_CONFIG_NAME)
                .onComplete(configsValue -> applyPurgeOfPastJobs(adminRequest, configsValue.result()));
          }
        });
    return Future.succeededFuture();
  }

  private void applyPurgeOfPastJobs(AdminRequest adminRequest, String purgeSetting) {
    Period ageForDeletion = getPeriod(purgeSetting, 3, "MONTHS");
    LocalDateTime untilDate = SettableClock.getLocalDateTime().minus(ageForDeletion).truncatedTo(ChronoUnit.MINUTES);
    logger.info("Running timer process: purging aged logs from before " + untilDate);
    adminRequest.moduleStorageAccess().purgePreviousJobsByAge(untilDate)
        .onSuccess(x -> adminRequest.routingContext().response().setStatusCode(204).end())
        .onFailure(e -> adminRequest.routingContext().response().setStatusCode(500).end(e.getMessage()));
  }

  private Future<Void> getJobLog(Vertx vertx, RoutingContext routingContext) {
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    return adminRequest.legacyHarvesterAccess().getJobLog(routingContext)
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    return adminRequest.legacyHarvesterAccess().getFailedRecords(routingContext).onComplete(getResponse -> {
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    return adminRequest.legacyHarvesterAccess().getFailedRecord(adminRequest).onComplete((getResponse -> {
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
            String harvestable_startedDate = harvestable.result().jsonObject().getString("lastHarvestStarted");
            // Use the `started` datetime on the HarvestJob, if localindices set it in time as the job was started
            //  otherwise use the `harvestStartedDate` property of the status message that localindices POSTs after the job ends
            //  but then strip the milliseconds or log filtering by date will fail.
            String harvestStartedDate =
                jobStatus.isEmpty()
                    || jobStatus.getString(HarvestJobField.STARTED.propertyName()) == null
                    ? harvestable_startedDate.substring(0, 19) + harvestable_startedDate.substring(23)
                    : jobStatus.getString(HarvestJobField.STARTED.propertyName());
            logger.info("Looking for logs by start date: " + harvestStartedDate);
            Future.all(
                    legacyStorage.getJobLog(harvestableId, harvestStartedDate.substring(0, 19)),
                    legacyStorage.getFailedRecords(harvestableId, 0, 10000))
                .onComplete(logResults -> {
                  HttpResponseImpl<Buffer> logsResponse = logResults.result().resultAt(0);
                  ProcessedHarvesterResponseGet failuresResponse = logResults.result().resultAt(1);
                  ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
                  HarvestJob job =
                      HarvestJob.fromHarvestableJson(harvestable.result().jsonObject());
                  List<StoredEntity> failedRecords = failedRecordsFromLegacyHarvesterJson(job.getId(),
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
                      .onComplete(jobStored -> Future.all(
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
            logger.error("{} {}", harvestable.result().statusCode(), harvestable.cause().getMessage());
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    String fromDateTime = adminRequest.queryParam("from");
    String untilDateTime = adminRequest.queryParam("until");
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
          .makeSqlFromCqlQuery(routingContext, adminRequest.moduleStorageAccess().schemaDotTable(Tables.harvest_job))
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
    return adminRequest.moduleStorageAccess().getPreviousJobs(query.getQueryWithLimits())
        .compose(jobs -> {
          for (HarvestJob job : jobs) {
            previousJobs.add(job.asJson());
          }
          return adminRequest.moduleStorageAccess().getCount(query.getCountingSql());
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    HarvestJob job =
        HarvestJob.fromHarvestJobJson(adminRequest.bodyAsJson());
    return adminRequest.moduleStorageAccess().storeHarvestJob(job)
        .onComplete(harvestJobId -> {
          if (harvestJobId.result() == null) {
            responseText(routingContext, 400).end("Failed to insert harvest job run: " + harvestJobId.cause().getMessage());
          } else {
            responseJson(routingContext, 201).end(job.asJson().encodePrettily());
          }
        }).mapEmpty();
  }

  private Future<Void> getPreviousJobById(Vertx vertx, RoutingContext routingContext) {
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    UUID id = UUID.fromString(adminRequest.requestParam("id"));
    return adminRequest.moduleStorageAccess().getPreviousJobById(id)
        .onComplete(harvestJob -> {
          if (harvestJob.result() == null) {
            responseText(routingContext, 404).end("Found no job with id " + id);
          } else {
            responseJson(routingContext, 200).end(harvestJob.result().asJson().encodePrettily());
          }
        }).mapEmpty();
  }

  private Future<Void> deletePreviousJob(Vertx vertx, RoutingContext routingContext) {
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    UUID id = UUID.fromString(adminRequest.requestParam("id"));
    return adminRequest.moduleStorageAccess().deletePreviousJob(id)
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    String contentType = routingContext.request().getHeader("Accept");
    logger.info("Request for previous logs in " + contentType + ".");
    String idQueryParameter = adminRequest.requestParam("id");
    UUID id = (idQueryParameter != null ? UUID.fromString(idQueryParameter) : null);
    String fromDateTime = adminRequest.queryParam("from");
    String untilDateTime = adminRequest.queryParam("until");
    String timeRange = null;
    if (fromDateTime != null && untilDateTime != null) {
      timeRange = "time_stamp >= '" + fromDateTime + "' AND time_stamp <= '" + untilDateTime + "'";
    } else if (fromDateTime != null) {
      timeRange = "time_stamp >= '" + fromDateTime + "'";
    } else if (untilDateTime != null) {
      timeRange = "time_stamp <= '" + untilDateTime + "'";
    }

    ModuleStorageAccess moduleStorage = adminRequest.moduleStorageAccess();
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

  public Future<Void> postPreviousJobLog(Vertx vertx, RoutingContext routingContext) {
    AdminRequest adminRequest = new RequestUnvalidated(vertx, routingContext);
    UUID id = UUID.fromString(adminRequest.requestParam("id"));
    return adminRequest
        .moduleStorageAccess()
        .storeLogStatements(id, adminRequest.bodyAsString())
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    ModuleStorageAccess moduleStorageAccess = adminRequest.moduleStorageAccess();
    SqlQuery queryFromCql = RecordFailure.entity().makeSqlFromCqlQuery(
            routingContext, moduleStorageAccess.schemaDotTable(Tables.record_failure_view))
        .withDefaultLimit("100");
    String jobId = adminRequest.requestParam("id");
    String from = adminRequest.requestParam("from");
    System.out.println("ID-NE: from: " + from);
    String until = adminRequest.requestParam("until");

    String timeRange = null;
    if (from != null && until != null) {
      timeRange = " (time_stamp >= '" + from
          + "'  AND time_stamp <= '" + until + "') ";
    } else if (from != null) {
      timeRange = " time_stamp >= '" + from + "' ";
    } else if (until != null) {
      timeRange = " time_stamp <= '" + until + "' ";
    }

    if (jobId != null) {
      queryFromCql.withAdditionalWhereClause("harvest_job_id = '" + jobId + "'");
    }
    if (timeRange != null) {
      queryFromCql.withAdditionalWhereClause(timeRange);
    }

    return moduleStorageAccess.getFailedRecordsForPreviousJobs(queryFromCql).onComplete(
        failuresList -> {
          if (failuresList.succeeded()) {
            JsonObject responseJson = new JsonObject();
            JsonArray recordFailures = new JsonArray();
            responseJson.put("failedRecords", recordFailures);
            List<RecordFailure> failures = failuresList.result();
            for (RecordFailure failure : failures) {
              recordFailures.add(failure.asJson());
            }
            moduleStorageAccess.getCount(queryFromCql.getCountingSql()).onComplete(
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);

    UUID id = UUID.fromString(adminRequest.requestParam("id"));
    return adminRequest.moduleStorageAccess().getFailedRecordForPreviousJob(id).onComplete(
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    UUID id = UUID.fromString(adminRequest.requestParam("id"));
    JsonObject failedRecordsJson = adminRequest.bodyAsJson();
    List<StoredEntity> failedRecords =
        failedRecordsFromHarvesterAdminJson(id, failedRecordsJson.getJsonArray("failedRecords"));
    return adminRequest.moduleStorageAccess().storeFailedRecords(id, failedRecords)
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
    AdminRequest adminRequest = new RequestValidated(vertx, routingContext);
    int count = 1;
    if (adminRequest.requestParam("count") != null) {
      count = Math.min(Integer.parseInt(adminRequest.requestParam("count")), 100);
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
