package org.folio.harvesteradmin.service;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
import org.folio.harvesteradmin.service.fileimport.FileQueue;
import org.folio.harvesteradmin.service.fileimport.XmlFilesImportVerticle;
import org.folio.harvesteradmin.utils.SettableClock;
import org.folio.okapi.common.HttpResponse;
import org.folio.tlib.RouterCreator;
import org.folio.tlib.TenantInitHooks;
import org.folio.tlib.postgres.PgCqlException;
import org.folio.tlib.util.TenantUtil;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.getIntOrDefault;
import static org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage.pagingPlainText;
import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.utils.Miscellaneous.getPeriod;
import static org.folio.okapi.common.HttpResponse.*;


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

        // Migrated entities, configs stored in pg
        routerBuilder
                .operation("postImportConfig")
                .handler(ctx -> _postImportConfig(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getImportConfigs")
                .handler(ctx -> _getImportConfigs(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("getImportConfig")
                .handler(ctx -> _getImportConfigById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);

        routerBuilder
                .operation("_postStep")
                .handler(ctx -> _postStep(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_getSteps")
                .handler(ctx -> _getSteps(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_getStep")
                .handler(ctx -> _getStepById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_getScript")
                .handler(ctx -> _getScript(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_getTransformation")
                .handler(ctx -> _getTransformationById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_getTransformations")
                .handler(ctx -> _getTransformations(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_postTransformation")
                .handler(ctx -> _postTransformation(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_getTsas")
                .handler(ctx -> _getTransformationSteps(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_getTsa")
                .handler(ctx -> _getTransformationStepById(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);
        routerBuilder
                .operation("_postTsa")
                .handler(ctx -> _postTransformationStep(vertx, ctx)
                        .onFailure(cause -> exceptionResponse(cause, ctx)))
                .failureHandler(this::routerExceptionResponse);


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
                        final JsonObject jobStatusOverrides =
                                (routingContext.body() != null && routingContext.body().asJsonObject() != null
                                        ? routingContext.body().asJsonObject() : new JsonObject());
                        HarvestJob job =
                                new HarvestJob().fromHarvestableJson(harvestable.result().jsonObject(), jobStatusOverrides);
                        logger.info("Looking for logs by start date: " + job.record.started());
                        CompositeFuture.all(
                                        legacyStorage.getJobLog(harvestableId, job.record.started()),
                                        legacyStorage.getFailedRecords(harvestableId, 0, 10000))
                                .onComplete(logResults -> {
                                    HttpResponseImpl<Buffer> logsResponse = logResults.result().resultAt(0);
                                    ProcessedHarvesterResponseGet failuresResponse = logResults.result().resultAt(1);
                                    ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
                                    List<Entity> failedRecords = failedRecordsFromLegacyHarvesterJson(job.record.id(),
                                            failuresResponse.jsonObject().getJsonArray("failedRecords"));
                                    moduleStorage.storeHarvestJob(job)
                                            .onComplete(jobStored -> CompositeFuture.all(
                                                    moduleStorage.storeLogStatements(job.record.id(), logsResponse.bodyAsString()),
                                                    moduleStorage.storeFailedRecords(job.record.id(), failedRecords)
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

    private static List<Entity> failedRecordsFromLegacyHarvesterJson(UUID harvestJobId, JsonArray failedRecords) {
        List<Entity> list = new ArrayList<>();
        for (Object rec : failedRecords) {
            JsonObject failedRecord = (JsonObject) rec;
            list.add(new RecordFailure().fromLegacyHarvesterJson(harvestJobId, failedRecord));
        }
        return list;
    }

    private static List<Entity> failedRecordsFromHarvesterAdminJson(UUID harvestJobId, JsonArray failedRecords) {
        List<Entity> list = new ArrayList<>();
        for (Object rec : failedRecords) {
            JsonObject failedRecord = (JsonObject) rec;
            list.add(new RecordFailure().fromHarvesterAdminJson(harvestJobId, failedRecord));
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
            query = new HarvestJob()
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
        HarvestJob job = new HarvestJob().fromJson(routingContext.body().asJsonObject());
        return new ModuleStorageAccess(vertx, tenant).storeEntity(job)
                .onSuccess(configId ->
                        responseJson(routingContext, 201).end(job.asJson().encodePrettily()))
                .mapEmpty();
    }


    /**
     * Migrated implementations of existing Harvester wrapping APIs
     */

    private Future<Void> _getEntities(Vertx vertx, RoutingContext routingContext, Entity entity) {
        String tenant = TenantUtil.tenant(routingContext);
        ModuleStorageAccess moduleStorage = new ModuleStorageAccess(vertx, tenant);
        SqlQuery query;
        try {
            query = entity
                    .makeSqlFromCqlQuery(routingContext, moduleStorage.schemaDotTable(entity.table()));
        } catch (PgCqlException pce) {
            responseText(routingContext, 400)
                    .end("Could not execute query to retrieve " + entity.jsonCollectionName() + ": " + pce.getMessage() + " Request:" + routingContext.request().absoluteURI());
            return Future.succeededFuture();
        } catch (Exception e) {
            return Future.failedFuture(e.getMessage());
        }
        return moduleStorage.getEntities(query.getQueryWithLimits(), entity).onComplete(
                result -> {
                    if (result.succeeded()) {
                        JsonObject responseJson = new JsonObject();
                        JsonArray jsonRecords = new JsonArray();
                        responseJson.put(entity.jsonCollectionName(), jsonRecords);
                        List<Entity> recs = result.result();
                        for (Entity rec : recs) {
                            jsonRecords.add(rec.asJson());
                        }
                        moduleStorage.getCount(query.getCountingSql()).onComplete(
                                count -> {
                                    responseJson.put("totalRecords", count.result());
                                    responseJson(routingContext, 200).end(responseJson.encodePrettily());
                                }
                        );
                    } else {
                        responseText(routingContext, 500)
                                .end("Problem retrieving jobs: " + result.cause().getMessage());
                    }
                }
        ).mapEmpty();
    }

    private Future<Void> _getEntityById(Vertx vertx, RoutingContext routingContext, Entity entity) {
        String tenant = TenantUtil.tenant(routingContext);
        RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
        UUID id = UUID.fromString(params.pathParameter("id").getString());
        return new ModuleStorageAccess(vertx, tenant).getEntityById(id, entity)
                .onSuccess(instance -> {
                    if (instance == null) {
                        responseText(routingContext, 404).end(entity.entityName() + " " + id + " not found.");
                    } else {
                        responseJson(routingContext, 200).end(instance.asJson().encodePrettily());
                    }
                })
                .mapEmpty();
    }


    private Future<Void> _postImportConfig(Vertx vertx, RoutingContext routingContext) {
        String tenant = TenantUtil.tenant(routingContext);
        ImportConfig importConfig = new ImportConfig().fromJson(routingContext.body().asJsonObject());
        return new ModuleStorageAccess(vertx, tenant).storeEntity(importConfig)
                .onSuccess(configId ->
                        responseJson(routingContext, 201).end(importConfig.asJson().encodePrettily()))
                .mapEmpty();
    }

    private Future<Void> _getImportConfigs(Vertx vertx, RoutingContext routingContext) {
        return _getEntities(vertx, routingContext, new ImportConfig());
    }

    private Future<Void> _getImportConfigById(Vertx vertx, RoutingContext routingContext) {
        return _getEntityById(vertx, routingContext, new ImportConfig());
    }

    private Future<Void> _postStep(Vertx vertx, RoutingContext routingContext) {
        String tenant = TenantUtil.tenant(routingContext);
        Step step = new Step().fromJson(routingContext.body().asJsonObject());
        return new ModuleStorageAccess(vertx, tenant).storeEntity(step)
                .onSuccess(stepId ->
                        responseJson(routingContext, 201).end(step.asJson().encodePrettily()))
                .mapEmpty();
    }

    private Future<Void> _getSteps(Vertx vertx, RoutingContext routingContext) {
        return _getEntities(vertx, routingContext, new Step());
    }

    private Future<Void> _getStepById(Vertx vertx, RoutingContext routingContext) {
        return _getEntityById(vertx, routingContext, new Step());
    }

    private Future<Void> _getScript(Vertx vertx, RoutingContext routingContext) {
        String tenant = TenantUtil.tenant(routingContext);
        return new ModuleStorageAccess(vertx, tenant).getScript(routingContext)
                .onSuccess(script -> responseText(routingContext, 200).end(script))
                .mapEmpty();
    }

    private Future<Void> _postTransformation(Vertx vertx, RoutingContext routingContext) {
        String tenant = TenantUtil.tenant(routingContext);
        Entity transformation = new Transformation().fromJson(routingContext.body().asJsonObject());
        return new ModuleStorageAccess(vertx, tenant).storeEntity(transformation)
                .onSuccess(id ->
                        responseJson(routingContext, 201).end(transformation.asJson().encodePrettily()))
                .mapEmpty();
    }

    private Future<Void> _getTransformationById(Vertx vertx, RoutingContext routingContext) {
        return _getEntityById(vertx, routingContext, new Transformation());
    }

    private Future<Void> _getTransformations(Vertx vertx, RoutingContext routingContext) {
        return _getEntities(vertx, routingContext, new Transformation());
    }

    private Future<Void> _postTransformationStep(Vertx vertx, RoutingContext routingContext) {
        String tenant = TenantUtil.tenant(routingContext);
        Entity transformationStep = new TransformationStep().fromJson(routingContext.body().asJsonObject());
        return new ModuleStorageAccess(vertx, tenant).storeEntity(transformationStep)
                .onSuccess(id ->
                        responseJson(routingContext, 201).end(transformationStep.asJson().encodePrettily()))
                .mapEmpty();
    }

    private Future<Void> _getTransformationStepById(Vertx vertx, RoutingContext routingContext) {
        return _getEntityById(vertx, routingContext, new TransformationStep());
    }

    private Future<Void> _getTransformationSteps(Vertx vertx, RoutingContext routingContext) {
        return _getEntities(vertx, routingContext, new TransformationStep());
    }

    /**
     * End of migrated implementations of Harvester wrapping APIs
     */

    private Future<Void> getPreviousJobById(Vertx vertx, RoutingContext routingContext) {
        return _getEntityById(vertx, routingContext, new HarvestJob());
    }

    private Future<Void> deletePreviousJob(Vertx vertx, RoutingContext routingContext) {
        String tenant = TenantUtil.tenant(routingContext);
        RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
        UUID id = UUID.fromString(params.pathParameter("id").getString());
        return new ModuleStorageAccess(vertx, tenant).deletePreviousJob(id)
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
        SqlQuery queryFromCql = new LogLine()
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
                                                        + harvestJob.result().record.name() + ", has no logs.");
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
                                                        + harvestJob.result().record.name() + ", has no logs.");
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

        SqlQuery queryFromCql = new RecordFailure().makeSqlFromCqlQuery(
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
        List<Entity> failedRecords =
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

    private void stageXmlRecordsFile(Vertx vertx, RoutingContext routingContext) {

        final long fileStartTime = System.currentTimeMillis();
        String tenant = TenantUtil.tenant(routingContext);
        String jobConfigId = routingContext.pathParam("id");
        String fileName = routingContext.queryParam("filename").stream().findFirst().orElse(UUID.randomUUID() + ".xml");
        Buffer xmlContent = Buffer.buffer(routingContext.body().asString());
        System.out.println("Staging XML file, current thread is " + Thread.currentThread().getName());

        new LegacyHarvesterStorage(vertx, tenant).getConfigRecordById(HARVESTER_HARVESTABLES_PATH, jobConfigId)
                .onComplete(resp -> {
                    if (resp.result().found()) {
                        new FileQueue(vertx, tenant, jobConfigId).addNewFile(fileName, xmlContent);
                        XmlFilesImportVerticle.launchVerticle(tenant, jobConfigId, routingContext);
                        responseText(routingContext, 200).end("File queued for processing in ms " + (System.currentTimeMillis() - fileStartTime));
                    } else {
                        responseError(routingContext, 404, "Error: No harvest config with id [" + jobConfigId + "] found.");
                    }
                })
                .onFailure(e -> responseError(routingContext, 500, "Error: " + e.getMessage()));

    }
}
