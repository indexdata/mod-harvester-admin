package org.folio.harvesteradmin.service;

import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.getIntOrDefault;
import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.pagingPlainText;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.utils.Miscellaneous.getPeriod;
import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;
import static org.folio.okapi.common.HttpResponse.responseText;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.impl.HttpResponseImpl;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.foliodata.SettingsClient;
import org.folio.harvesteradmin.legacydata.JobLauncher;
import org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage;
import org.folio.harvesteradmin.legacydata.responsehandlers.ProcessedHarvesterResponseGet;
import org.folio.harvesteradmin.foliodata.ConfigurationsClient;
import org.folio.harvesteradmin.moduledata.*;
import org.folio.harvesteradmin.moduledata.database.ModuleStorageAccess;
import org.folio.harvesteradmin.moduledata.database.SqlQuery;
import org.folio.harvesteradmin.moduledata.database.Tables;
import org.folio.harvesteradmin.service.fileimport.FileQueue;
import org.folio.harvesteradmin.service.fileimport.JobHandler;
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
            routerBuilder.rootHandler(BodyHandler.create().setBodyLimit(30000000));
            handlers(vertx, routerBuilder);
            return routerBuilder.createRouter();
        });
    }

    private void handlers(Vertx vertx, RouterBuilder routerBuilder) {

        routerBuilder
                .operation("getHarvestables")
                .handler(ctx -> getConfigRecords(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getHarvestable")
                .handler(ctx -> getConfigRecordById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postHarvestable")
                .handler(ctx -> postConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postHarvestableXmlBulk")
                .handler(ctx -> postConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postHarvestableOaiPmh")
                .handler(ctx -> postConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("putHarvestable")
                .handler(ctx -> putConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("putHarvestableXmlBulk")
                .handler(ctx -> putConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("putHarvestableOaiPmh")
                .handler(ctx -> putConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("deleteHarvestable")
                .handler(ctx -> deleteConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("getJobLog")
                .handler(ctx -> getJobLog(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("getFailedRecords")
                .handler(ctx -> getFailedRecords(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("getFailedRecord")
                .handler(ctx -> getFailedRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("storeJobLog")
                .handler(ctx -> pullJobAndSaveItsLogs(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("storeJobLogWithPostedStatus")
                .handler(ctx -> pullJobAndSaveItsLogs(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("getPreviousJobs")
                .handler(ctx -> getPreviousJobs(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getPreviousJob")
                .handler(ctx -> getPreviousJobById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("deletePreviousJob")
                .handler(ctx -> deletePreviousJob(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postPreviousJob")
                .handler(ctx -> postPreviousJob(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getPreviousJobLog")
                .handler(ctx -> getPreviousJobLog(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postPreviousJobLog")
                .handler(ctx -> postPreviousJobLog(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getFailedRecordsForPreviousJob")
                .handler(ctx -> getFailedRecordsForPreviousJobs(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postFailedRecords")
                .handler(ctx -> postFailedRecords(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getFailedRecordForPreviousJob")
                .handler(ctx -> getFailedRecordForPreviousJob(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getFailedRecordsForPreviousJobs")
                .handler(ctx -> getFailedRecordsForPreviousJobs(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("getStorages")
                .handler(ctx -> getConfigRecords(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getStorage")
                .handler(ctx -> getConfigRecordById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postStorage")
                .handler(ctx -> postConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("putStorage")
                .handler(ctx -> putConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("deleteStorage")
                .handler(ctx -> deleteConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("getTransformations")
                .handler(ctx -> getConfigRecords(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getTransformation")
                .handler(ctx -> getConfigRecordById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postTransformation")
                .handler(ctx -> postConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("putTransformation")
                .handler(ctx -> putConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("deleteTransformation")
                .handler(ctx -> deleteConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("getSteps")
                .handler(ctx -> getConfigRecords(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getStep")
                .handler(ctx -> getConfigRecordById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postStep")
                .handler(ctx -> postConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("putStep")
                .handler(ctx -> putConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("deleteStep")
                .handler(ctx -> deleteConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getScript")
                .handler(ctx -> getScript(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("putScript")
                .handler(ctx -> putScript(vertx, ctx));
        routerBuilder
                .operation("getTsas")
                .handler(ctx -> getConfigRecords(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getTsa")
                .handler(ctx -> getConfigRecordById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("postTsa")
                .handler(ctx -> postConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("deleteTsa")
                .handler(ctx -> deleteConfigRecord(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("startJob")
                .handler(ctx -> startJob(vertx, ctx))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("stopJob")
                .handler(ctx -> stopJob(vertx, ctx))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("purgeAgedLogs")
                .handler(ctx -> purgeAgedLogs(vertx, ctx))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("getIds")
                .handler(this::generateIds);

        routerBuilder
                .operation("importXmlRecords")
                .handler(ctx -> stageXmlRecords(vertx, ctx))
                .failureHandler(this::routerExceptionResponse);

    }

    private void exceptionResponse(Throwable cause, RoutingContext routingContext) {
        if (cause.getMessage().toLowerCase().contains("could not find")) {
            HttpResponse.responseError(routingContext, 404, cause.getMessage());
        } else {
            HttpResponse.responseError(routingContext, 404, cause.getMessage());
        }

    }

    /**
     * Returns request validation exception, potentially with improved error message if problem was
     * an error in a polymorph schema, like in `harvestable` of type `oaiPmh` vs `xmlBulk`.
     */
    private void routerExceptionResponse(RoutingContext ctx) {
        String message = null;
        if (ctx.failure() != null) message = ctx.failure().getMessage();
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

    private void putScript(Vertx vertx, RoutingContext routingContext) {
        String tenant = TenantUtil.tenant(routingContext);
        LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
        legacyStorage.putScript(routingContext)
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

    private void purgeAgedLogs(Vertx vertx, RoutingContext routingContext) {
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
    }

    private void applyPurgeOfPastJobs(Vertx vertx, RoutingContext routingContext, String purgeSetting) {
        Period ageForDeletion = getPeriod(purgeSetting, 3, "MONTHS");
        LocalDateTime untilDate = SettableClock.getLocalDateTime().minus(ageForDeletion).truncatedTo(ChronoUnit.MINUTES);
        logger.info("Running timer process: purging aged logs from before " + untilDate);
        String tenant = TenantUtil.tenant(routingContext);
        ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
        moduleStorage.purgePreviousJobsByAge(untilDate)
                .onComplete(x -> routingContext.response().setStatusCode(204).end()).mapEmpty();
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
            return Future.failedFuture(e.getMessage());
        }
        return moduleStorage.getPreviousJobs(query.getQueryWithLimits()).onComplete(
                jobsList -> {
                    if (jobsList.succeeded()) {
                        JsonObject responseJson = new JsonObject();
                        JsonArray previousJobs = new JsonArray();
                        responseJson.put("previousJobs", previousJobs);
                        List<HarvestJob> jobs = jobsList.result();
                        for (HarvestJob job : jobs) {
                            previousJobs.add(job.asJson());
                        }
                        moduleStorage.getCount(query.getCountingSql()).onComplete(
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

    public Future<Void> postPreviousJobLog(Vertx vertx, RoutingContext routingContext) {
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

    private void generateIds(RoutingContext routingContext) {
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
    }

    private final static ConcurrentMap<String, ConcurrentMap<String, String>> fileProcessorVerticles = new ConcurrentHashMap<>();

    private void initializeFileQueueProcessor(String tenant, String jobId, RoutingContext routingContext) {
        fileProcessorVerticles.putIfAbsent(tenant, new ConcurrentHashMap<>());
        String previousMapping = fileProcessorVerticles.get(tenant).putIfAbsent(jobId, "initializing");
        if (previousMapping == null) {
            Vertx vertx = Vertx.vertx(new VertxOptions().setMaxWorkerExecuteTime(10).setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES));
            vertx.deployVerticle(new JobHandler(tenant, jobId, vertx, routingContext), new DeploymentOptions()
                    .setWorkerPoolSize(1).setMaxWorkerExecuteTime(10).setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES)).onComplete(
                    started -> {
                        if (started.succeeded()) {
                            System.out.println("ID-NE: started verticle for " + tenant + " and job ID " + jobId);
                            System.out.println("ID-NE: current thread " + Thread.currentThread().getName());
                            fileProcessorVerticles.get(tenant).put(jobId, started.result());
                            System.out.println("ID-NE: deployed verticles: " + vertx.deploymentIDs());
                        } else {
                            System.out.println("ID-NE: Couldn't start file processor threads for tenant " + tenant + " and jobID " + jobId);
                        }
                    });
        } else {
            System.out.println("ID-NE: Continuing with already existing verticle for tenant " + tenant + " and job ID " + jobId);
        }
    }

    private void stageXmlRecords(Vertx vertx, RoutingContext routingContext) {

        final long fileStartTime = System.currentTimeMillis();
        String tenant = TenantUtil.tenant(routingContext);
        String jobId = routingContext.pathParam("id");
        String fileName = routingContext.queryParam("filename").stream().findFirst().orElse(UUID.randomUUID() + ".xml");
        Buffer xmlContent = Buffer.buffer(routingContext.body().asString());
        System.out.println("Staging XML file, current thread is " + Thread.currentThread().getName());

        new LegacyHarvesterStorage(vertx, tenant).getConfigRecordById(HARVESTER_HARVESTABLES_PATH, jobId)
                .onComplete(resp -> {
                    if (resp.result().found()) {
                        new FileQueue(vertx, tenant, jobId).addNewFile(fileName, xmlContent);
                        initializeFileQueueProcessor(tenant, jobId, routingContext);
                        responseText(routingContext, 200).end("File queued for processing in ms " + (System.currentTimeMillis() - fileStartTime));
                    } else {
                        responseError(routingContext, 404, "Error: No harvest config with id [" + jobId + "] found.");
                    }
                })
                .onFailure(e -> responseError(routingContext, 500, "Error: " + e.getMessage()));

    }
}
