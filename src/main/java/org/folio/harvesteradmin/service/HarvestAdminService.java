package org.folio.harvesteradmin.service;

import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;
import static org.folio.okapi.common.HttpResponse.responseText;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import java.util.List;
import java.util.UUID;
import org.folio.harvesteradmin.dataaccess.LegacyHarvesterStorage;
import org.folio.harvesteradmin.moduledata.HarvestJob;
import org.folio.harvesteradmin.modulestorage.HarvestAdminStorage;
import org.folio.tlib.RouterCreator;
import org.folio.tlib.TenantInitHooks;
import org.folio.tlib.util.TenantUtil;

public class HarvestAdminService implements RouterCreator, TenantInitHooks {

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
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postHarvestable")
        .handler(ctx -> postConfigRecord(vertx, ctx));
    routerBuilder
        .operation("deleteHarvestable")
        .handler(ctx -> deleteConfigRecord(vertx, ctx));

    routerBuilder
        .operation("getJobLog")
            .handler(ctx -> getJobLog(vertx, ctx));

    routerBuilder
        .operation("storeJobLog")
            .handler(ctx -> storeJobLog(vertx, ctx));

    routerBuilder
        .operation("getPreviousJobs")
            .handler(ctx -> getPreviousJobs(vertx, ctx));
    routerBuilder
        .operation("getPreviousJob")
            .handler(ctx -> getPreviousJobById(vertx, ctx));

    routerBuilder
        .operation("getPreviousJobLog")
            .handler(ctx -> getPreviousJobLog(vertx, ctx));

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
            .handler(ctx -> putConfigRecord(vertx, ctx));
    routerBuilder
        .operation("deleteStorage")
        .handler(ctx -> deleteConfigRecord(vertx, ctx));

    routerBuilder
        .operation("getTransformations")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getTransformation")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postTransformation")
        .handler(ctx -> postConfigRecord(vertx, ctx));
    routerBuilder
        .operation("putTransformation")
            .handler(ctx -> putConfigRecord(vertx, ctx));
    routerBuilder
        .operation("deleteTransformation")
        .handler(ctx -> deleteConfigRecord(vertx, ctx));

    routerBuilder
        .operation("getSteps")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getStep")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postStep")
        .handler(ctx -> postConfigRecord(vertx, ctx));
    routerBuilder
        .operation("putStep")
        .handler(ctx -> putConfigRecord(vertx, ctx));
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
        .handler(ctx -> postConfigRecord(vertx, ctx));
    routerBuilder
        .operation("deleteTsa")
        .handler(ctx -> deleteConfigRecord(vertx, ctx));
  }

  @Override
  public Future<Void> postInit(Vertx vertx, String tenant, JsonObject tenantAttributes) {
    HarvestAdminStorage storage = new HarvestAdminStorage(vertx, tenant);
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
          responseText(routingContext, 200).end(response.result());
        })
        .onFailure(response -> {
          responseError(routingContext, 500, response.getMessage());
        })
        .mapEmpty();
  }

  private Future<Void> putScript(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.putScript(routingContext)
        .onComplete(response -> {
          responseText(routingContext, 204).end();
        })
        .onFailure(response -> {
          responseError(routingContext, 500, response.getMessage());
        })
        .mapEmpty();
  }

  /**
   * Gets the log for a job.
   */
  public Future<Void> getJobLog(Vertx vertx, RoutingContext routingContext) {
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
          responseError(routingContext, 404, failure.getMessage());
        })
        .mapEmpty();
  }

  /**
   * Stores a job log.
   */
  public Future<Void> storeJobLog(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.getJobLog(routingContext)
        .onComplete(response -> {
          if (response.succeeded()) {
            legacyStorage.getConfigRecordById(
                HARVESTER_HARVESTABLES_PATH,
                routingContext.request().getParam("id"))
                .onComplete(harvestableResult -> {
                  JsonObject harvestable = harvestableResult.result().jsonObject();
                  String log = response.result().bodyAsString();
                  HarvestAdminStorage storage = new HarvestAdminStorage(vertx, tenant);
                  HarvestJob harvestJob = HarvestJob.fromHarvestableJson(harvestable);
                  storage.storeHarvestJob(harvestJob).onComplete(harvestJobId -> {
                    storage.storeLogStatements(harvestJobId.result(), log)
                        .onComplete(done ->
                            responseText(
                                routingContext,
                                response.result().statusCode())
                                .end(log == null ? "No logs found for this job."
                                    : "Logs persisted in module storage."));
                  });
                });
          }
        })
        .onFailure(failure -> {
          responseError(routingContext, 404, failure.getMessage());
        })
        .mapEmpty();
  }

  private Future<Void> getPreviousJobs(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    HarvestAdminStorage storage = new HarvestAdminStorage(vertx, tenant);
    return storage.getPreviousJobs().onComplete(
        jobsList -> {
          JsonObject responseJson = new JsonObject();
          JsonArray previousJobs = new JsonArray();
          responseJson.put("previousJobs", previousJobs);
          List<HarvestJob> jobs = jobsList.result();
          for (HarvestJob job : jobs) {
            previousJobs.add(job.asJson());
          }
          responseJson(routingContext,200).end(responseJson.encodePrettily());
        }
    ).mapEmpty();
  }

  private Future<Void> getPreviousJobById(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    HarvestAdminStorage storage = new HarvestAdminStorage(vertx, tenant);
    return storage.getPreviousJobById(id)
        .onComplete(harvestJob -> {
          if (harvestJob.result() == null) {
            responseText(routingContext, 404).end("Found no job with id " + id);
          } else {
            responseJson(routingContext, 200).end(harvestJob.result().asJson().encodePrettily());
          }
        }).mapEmpty();
  }

  private Future<Void> getPreviousJobLog(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    HarvestAdminStorage storage = new HarvestAdminStorage(vertx, tenant);
    return storage.getPreviousJobLog(id)
        .onComplete(jobLog -> {
          if (jobLog.result().length() == 0) {
            storage.getPreviousJobById(id).onComplete(harvestJob -> {
              if (harvestJob.result() == null) {
                responseText(routingContext, 404)
                    .end("Found no previous job with ID " + id);
              } else {
                responseText(routingContext, 200)
                    .end("Previous job with ID "
                        + id
                        + ", "
                        + harvestJob.result().name()
                        + ", has no logs.");
              }
            });
          } else {
            responseText(routingContext, 200).end(jobLog.result());
          }
        }).mapEmpty();
  }

  /* Methods accessing modules local storage.
  private Future<Void> getBook(Vertx vertx, RoutingContext ctx) {
    String tenant = TenantUtil.tenant(ctx);

    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    UUID id = UUID.fromString(params.pathParameter("id").getString());
    HarvestAdminStorage storage = new HarvestAdminStorage(vertx, tenant);
    return storage.getBook(id).map(book -> {
      if (book == null) {
        responseError(ctx, 404, "Not found " + id);
      } else {
        responseJson(ctx, 200).end(JsonObject.mapFrom(book).encode());
      }
      return null;
    });
  }

  private Future<Void> postBook(Vertx vertx, RoutingContext ctx) {
    String tenant = TenantUtil.tenant(ctx);
    HarvestAdminStorage storage = new HarvestAdminStorage(vertx, tenant);
    Book book = ctx.body().asPojo(Book.class);
    return storage.postBook(book).map(res -> {
      ctx.response().setStatusCode(204).end();
      return null;
    });
  }
   */

}
