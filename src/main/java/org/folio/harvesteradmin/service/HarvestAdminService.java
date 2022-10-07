package org.folio.harvesteradmin.service;

import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;
import static org.folio.okapi.common.HttpResponse.responseText;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.dataaccess.LegacyHarvesterStorage;
import org.folio.harvesteradmin.moduledata.Book;
import org.folio.harvesteradmin.modulestorage.HarvestAdminStorage;
import org.folio.tlib.RouterCreator;
import org.folio.tlib.TenantInitHooks;
import org.folio.tlib.util.TenantUtil;

public class HarvestAdminService implements RouterCreator, TenantInitHooks {

  protected static final Logger logger = LogManager.getLogger(HarvestAdminService.class);

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
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage());
        });
    routerBuilder
        .operation("deleteHarvestable")
        .handler(ctx -> deleteConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage());
        });

    routerBuilder
        .operation("getJobLog")
            .handler(ctx -> getJobLog(vertx, ctx));

    routerBuilder
        .operation("getStorages")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getStorage")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postStorage")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage());
        });
    routerBuilder
        .operation("putStorage")
            .handler(ctx -> putConfigRecord(vertx, ctx))
                .failureHandler(routingContext -> {
                  responseError(
                      routingContext,
                      routingContext.statusCode(),
                      routingContext.failure().getMessage()
                  );
                });
    routerBuilder
        .operation("deleteStorage")
        .handler(ctx -> deleteConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage()
          );
        });

    routerBuilder
        .operation("getTransformations")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getTransformation")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postTransformation")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage());
        });
    routerBuilder
        .operation("putTransformation")
            .handler(ctx -> putConfigRecord(vertx, ctx))
                .failureHandler(routingContext -> {
                  responseError(
                      routingContext,
                      routingContext.statusCode(),
                      routingContext.failure().getMessage());
                });
    routerBuilder
        .operation("deleteTransformation")
        .handler(ctx -> deleteConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage()
          );
        });

    routerBuilder
        .operation("getSteps")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getStep")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postStep")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage());
        });
    routerBuilder
        .operation("putStep")
        .handler(ctx -> putConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage());
        });
    routerBuilder
        .operation("deleteStep")
        .handler(ctx -> deleteConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage()
          );
        });

    routerBuilder
        .operation("getScript")
            .handler(ctx -> getScript(vertx, ctx));
    routerBuilder
        .operation("putScript")
        .handler(ctx -> putScript(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage());
        });

    routerBuilder
        .operation("getTsas")
        .handler(ctx -> getConfigRecords(vertx, ctx));
    routerBuilder
        .operation("getTsa")
        .handler(ctx -> getConfigRecordById(vertx, ctx));
    routerBuilder
        .operation("postTsa")
        .handler(ctx -> postConfigRecord(vertx, ctx))
        .failureHandler(routingContext -> {
          responseError(
              routingContext,
              routingContext.statusCode(),
              routingContext.failure().getMessage());
        });
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
        responseError(
            routingContext, response.statusCode(), response.errorMessage());
      }
      return null;
    });
  }

  private Future<Void> putConfigRecord(Vertx vertx, RoutingContext routingContext) {
    String tenant = TenantUtil.tenant(routingContext);
    LegacyHarvesterStorage legacyStorage = new LegacyHarvesterStorage(vertx, tenant);
    return legacyStorage.putConfigRecord(routingContext).map(response -> {
      if (response.wasOK()) {
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
          String log = response.result().bodyAsString();
          responseText(
              routingContext,
              response.result().statusCode())
              .end(log == null ? "No logs found for this job." : log);
        })
        .onFailure(failure -> {
          responseError(routingContext, 404, failure.getMessage());
        })
        .mapEmpty();
  }


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

}
