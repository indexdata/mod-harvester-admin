package org.folio.harvesteradmin.foliodata;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

class ConfigurationsClientTest extends ClientTestBase {

  Future<String> getStringValueFuture(RoutingContext routingContext, String key1, String key2) {
    return ConfigurationsClient.getStringValue(routingContext, key1, key2);
  }

}
