package org.folio.harvesteradmin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;
import io.vertx.core.http.HttpServerOptions;
import org.folio.harvesteradmin.legacydata.statics.LegacyServiceConfig;
import org.folio.harvesteradmin.service.HarvestAdminService;
import org.folio.harvesteradmin.service.harvest.Harvester;
import org.folio.okapi.common.Config;
import org.folio.tlib.RouterCreator;
import org.folio.tlib.api.HealthApi;
import org.folio.tlib.api.Tenant2Api;
import org.folio.tlib.postgres.TenantPgPool;


public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> promise) {

    TenantPgPool.setModule("mod-harvester-admin"); // Postgres - schema separation

    // listening port
    final int port = Integer.parseInt(Config.getSysConf("http.port", "port", "8081", config()));
    new LegacyServiceConfig();

    HarvestAdminService harvestAdminService = new HarvestAdminService();
    RouterCreator[] routerCreators = {
        harvestAdminService,
        new Tenant2Api(harvestAdminService),
        new HealthApi(),
    };

    HttpServerOptions so = new HttpServerOptions()
        .setHandle100ContinueAutomatically(true);
    RouterCreator.mountAll(vertx, routerCreators)
        .compose(router ->
            vertx.createHttpServer(so)
                .requestHandler(router)
                .listen(port).mapEmpty())
        .<Void>mapEmpty()
        .onComplete(promise);
  }

}
