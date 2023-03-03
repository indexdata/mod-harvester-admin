package org.folio.harvesteradmin.service;

import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
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
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.dataaccess.JobLauncher;
import org.folio.harvesteradmin.dataaccess.LegacyHarvesterStorage;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseGet;
import org.folio.harvesteradmin.moduledata.HarvestJob;
import org.folio.harvesteradmin.moduledata.HarvestJobField;
import org.folio.harvesteradmin.moduledata.LogLine;
import org.folio.harvesteradmin.moduledata.RecordFailure;
import org.folio.harvesteradmin.moduledata.SqlQuery;
import org.folio.harvesteradmin.modulestorage.Storage;
import org.folio.okapi.common.HttpResponse;
import org.folio.tlib.RouterCreator;
import org.folio.tlib.TenantInitHooks;
import org.folio.tlib.util.TenantUtil;

public class HarvestAdminService implements RouterCreator, TenantInitHooks {

  private static final Logger logger = LogManager.getLogger("harvester-admin");

  @Override
  public Future<Router> createRouter(Vertx vertx) {
    return RouterBuilder.create(vertx, "openapi/harvest-admin-1.0.yaml").map(routerBuilder -> {
      handlers(vertx, routerBuilder);
      return routerBuilder.createRouter();
    });
  }

  private void handlers(Vertx vertx, RouterBuilder routerBuilder) {

    routerBuilder
        .operation("getHarvestables")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getHarvestable")
        .handler(ctx -> getConfigRecordById(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);
    routerBuilder
        .operation("postHarvestable")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);
    routerBuilder
        .operation("postHarvestableXmlBulk")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);

    routerBuilder
        .operation("putHarvestable")
        .handler(ctx -> putConfigRecord(vertx, ctx));
    routerBuilder
        .operation("deleteHarvestable")
        .handler(ctx -> deleteConfigRecord(vertx, ctx));

    routerBuilder
        .operation("getJobLog")
            .handler(ctx -> getJobLog(vertx, ctx));

    routerBuilder
        .operation("getFailedRecords")
        .handler(ctx -> getFailedRecords(vertx, ctx));

    routerBuilder
        .operation("getFailedRecord")
        .handler(ctx -> getFailedRecord(vertx, ctx));

    routerBuilder
        .operation("storeJobLog")
        .handler(ctx -> pullJobAndSaveItsLogs(vertx, ctx));

    routerBuilder
        .operation("storeJobLogWithPostedStatus")
        .handler(ctx -> pullJobAndSaveItsLogs(vertx, ctx));

    routerBuilder
        .operation("getPreviousJobs")
        .handler(ctx -> getPreviousJobs(vertx, ctx)
            .onFailure(cause -> HttpResponse.responseError(ctx, 500, cause.getMessage())));
    routerBuilder
        .operation("getPreviousJob")
        .handler(ctx -> getPreviousJobById(vertx, ctx));
    routerBuilder
        .operation("deletePreviousJob")
        .handler(ctx -> deletePreviousJob(vertx, ctx));

    routerBuilder
        .operation("getPreviousJobLog")
        .handler(ctx -> getPreviousJobLog(vertx, ctx));
    routerBuilder
        .operation("getFailedRecordsForPreviousJob")
        .handler(ctx -> getFailedRecordsForPreviousJob(vertx, ctx));
    routerBuilder
        .operation("getFailedRecordForPreviousJob")
        .handler(ctx -> getFailedRecordForPreviousJob(vertx, ctx));

    routerBuilder
        .operation("getStorages")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getStorage")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postStorage")
        .handler(ctx -> postConfigRecord(vertx, ctx));
    routerBuilder
        .operation("putStorage")
            .handler(ctx -> putConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);
    routerBuilder
        .operation("deleteStorage")
        .handler(ctx -> deleteConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);

    routerBuilder
        .operation("getTransformations")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getTransformation")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postTransformation")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);
    routerBuilder
        .operation("putTransformation")
            .handler(ctx -> putConfigRecord(vertx, ctx));
    routerBuilder
        .operation("deleteTransformation")
        .handler(ctx -> deleteConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);

    routerBuilder
        .operation("getSteps")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getStep")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postStep")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);
    routerBuilder
        .operation("putStep")
        .handler(ctx -> putConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);
    routerBuilder
        .operation("deleteStep")
        .handler(ctx -> deleteConfigRecord(vertx, ctx));

    routerBuilder
        .operation("getScript")
            .handler(ctx -> getScript(vertx, ctx));
    routerBuilder
        .operation("putScript")
        .handler(ctx -> putScript(vertx, ctx));

    routerBuilder
        .operation("getTsas")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getTsa")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postTsa")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(this::routerExceptionResponse);
    routerBuilder
        .operation("deleteTsa")
        .handler(ctx -> deleteConfigRecord(vertx, ctx));

    routerBuilder
        .operation("startJob")
        .handler(ctx -> startJob(vertx, ctx));
    routerBuilder
        .operation("stopJob")
        .handler(ctx -> stopJob(vertx, ctx));

    routerBuilder
        .operation("getIds")
        .handler(ctx -> generateIds(ctx));

  }

  /**
   * Returns request validation exception, potentially with improved error message if problem was
   * an error in a polymorph schema, like in `harvestable` of type `oaiPmh` vs `xmlBulk`.
   */
  public void routerExceptionResponse(RoutingContext ctx) {
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
    Storage storage = new Storage(vertx, tenant);
    return storage.init(tenantAttributes);
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
        .onComplete(response -> responseText(routingContext, 204).end())
        .onFailure(response -> responseError(routingContext, 500, response.getMessage()))
        .mapEmpty();
  }

  private void startJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    JobLauncher launcher = new JobLauncher(vertx, tenant);
    launcher.startJob(routingContext);
  }

  private void stopJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    JobLauncher launcher = new JobLauncher(vertx, tenant);
    launcher.stopJob(routingContext);
  }

  private Future<Void> getJobLog(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.getJobLog(routingContext)
        .onComplete(response -> {
          if (response.succeeded()) {
            String log = response.result().bodyAsString();
            responseText(
                routingContext,
                response.result().statusCode())
                .end(log == null ? "No logs found for this job." : log);
          }
        })
        .onFailure(failure -> {
          int code = failure.getMessage().startsWith("Could not find") ? 404 : 500;
          responseError(routingContext, code, failure.getMessage());
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
            routingContext, getResponse.result().statusCode(), getResponse.cause().getMessage());
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
                    legacyStorage.getFailedRecords(harvestableId))
                .onComplete(logResults -> {
                  HttpResponseImpl<Buffer> logsResponse = logResults.result().resultAt(0);
                  ProcessedHarvesterResponseGet failuresResponse = logResults.result().resultAt(1);
                  Storage storage = new Storage(vertx, tenant);
                  HarvestJob job =
                      HarvestJob.fromHarvestableJson(harvestable.result().jsonObject());
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
                  storage.storeHarvestJob(job)
                          .onComplete(jobStored -> CompositeFuture.all(
                              storage.storeLogStatements(job.getId(),logsResponse.bodyAsString()),
                              storage.storeFailedRecords(job.getId(),
                                  failuresResponse.jsonObject().getJsonArray("failed-records"))
                          ).onComplete(
                              result -> {
                                if (result.succeeded()) {
                                  responseText(routingContext,200)
                                      .end("Saved job with logs and record failures if any.");
                                } else {
                                  responseError(routingContext,500,
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

  private Future<Void> getPreviousJobs(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    Storage storage = new Storage(vertx, tenant);

    String fromDateTime = routingContext.request().getParam("from");
    String untilDateTime = routingContext.request().getParam("until");
    String timeRange = null;
    if (fromDateTime != null && untilDateTime != null) {
      timeRange = " (started >= '" + fromDateTime + "'  AND started <= '" + untilDateTime + "') ";
    } else if (fromDateTime != null) {
      timeRange = " started >= '" + fromDateTime + "' ";
    } else if (untilDateTime != null) {
      timeRange = " started <= '" + untilDateTime + "' ";
    }

    SqlQuery query;
    try {
      query = HarvestJob.entity()
          .makeSqlFromCqlQuery(routingContext, storage.schemaDotTable(Storage.Table.harvest_job))
          .withAdditionalWhereClause(timeRange);
    } catch (Exception e) {
      return Future.failedFuture(e.getMessage());
    }
    return storage.getPreviousJobs(query.getQueryWithLimits()).onComplete(
        jobsList -> {
          if (jobsList.succeeded()) {
            JsonObject responseJson = new JsonObject();
            JsonArray previousJobs = new JsonArray();
            responseJson.put("previousJobs", previousJobs);
            List<HarvestJob> jobs = jobsList.result();
            for (HarvestJob job : jobs) {
              previousJobs.add(job.asJson());
            }
            storage.getCount(query.getCountingSql()).onComplete(
                count -> {
                  responseJson.put("totalRecords", count.result());
                  responseJson(routingContext, 200).end(responseJson.encodePrettily());
                }
            );
          } else {
            responseText(routingContext, 500)
                .end("Problem retrieving jobs: " + jobsList.cause().getMessage());
          }
        }
    ).mapEmpty();
  }

  private Future<Void> getPreviousJobById(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    Storage storage = new Storage(vertx, tenant);
    return storage.getPreviousJobById(id)
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
    Storage storage = new Storage(vertx, tenant);
    return storage.deletePreviousJob(id)
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

    Storage storage = new Storage(vertx, tenant);
    SqlQuery queryFromCql = LogLine.entity()
        .makeSqlFromCqlQuery(routingContext, storage.schemaDotTable(Storage.Table.log_statement))
        .withAdditionalWhereClause(timeRange);
    return storage.getLogsForPreviousJob(id, queryFromCql)
        .onComplete(jobLog -> {
          if (jobLog.succeeded()) {
            if (jobLog.result().length() == 0) {
              storage.getPreviousJobById(id).onComplete(harvestJob -> {
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

  private Future<Void> getFailedRecordsForPreviousJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    String offset = routingContext.request().getParam("offset");
    String limit = routingContext.request().getParam("limit");

    Storage storage = new Storage(vertx, tenant);
    return storage.getFailedRecordsForPreviousJob(id, offset, limit).onComplete(
        failuresList -> {
          if (failuresList.succeeded()) {
            JsonObject responseJson = new JsonObject();
            JsonArray recordFailures = new JsonArray();
            responseJson.put("failedRecords", recordFailures);
            List<RecordFailure> failures = failuresList.result();
            for (RecordFailure failure : failures) {
              recordFailures.add(failure.asJson());
            }
            storage.getCount("SELECT COUNT(*) as total_records "
                + "FROM " + storage.schemaDotTable(Storage.Table.record_failure) + " "
                + "WHERE harvest_job_id = '" + id + "'").onComplete(count -> {
                  responseJson.put("totalRecords", count.result());
                  responseJson(routingContext, 200).end(responseJson.encodePrettily());
                }
            );
          } else {
            responseText(routingContext, 500)
                .end("Problem retrieving jobs: " + failuresList.cause().getMessage());
          }
        }
    ).mapEmpty();
  }

  private Future<Void> getFailedRecordForPreviousJob(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    Storage storage = new Storage(vertx, tenant);
    return storage.getFailedRecordForPreviousJob(id).onComplete(
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

  private Future<Void> generateIds(RoutingContext routingContext) {
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    int count = 1;
    if (params.queryParameter("count") != null) {
      count = Math.min(params.queryParameter("count").getInteger(),100);
    }
    String response = "";
    for (int i = 0; i < count; i++) {
      response += LegacyHarvesterStorage.getRandomFifteenDigitString() + System.lineSeparator();
    }
    responseText(routingContext, 200)
        .end(response);
    return Future.succeededFuture();
  }

}
