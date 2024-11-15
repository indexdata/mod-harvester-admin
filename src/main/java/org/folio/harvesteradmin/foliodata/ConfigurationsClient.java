package org.folio.harvesteradmin.foliodata;

import static org.folio.util.StringUtil.cqlEncode;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.util.PercentCodec;

/**
 * Run GET /configurations/entries HTTP request to get configuration entry from mod-configuration.
 */
public class ConfigurationsClient {
  public static final String CONFIGURATIONS_PATH = "/configurations/entries";
  public static final String RECORDS = "configs";

  protected static final Logger logger =
      LogManager.getLogger(ConfigurationsClient.class);

  /**
   * Get the configuration value from mod-configuration for the module and configName.
   */
  public static Future<String> getStringValue(RoutingContext routingContext,
      String moduleName, String configName) {

    var cql = "module==" + cqlEncode(moduleName) + " and configName==" + cqlEncode(configName)
        + " and enabled=true";
    return Folio.okapiClient(routingContext)
        .get(CONFIGURATIONS_PATH + "?query=" + PercentCodec.encode(cql))
        .map(response ->
            new JsonObject(response).getJsonArray(RECORDS).getJsonObject(0).getString("value"))
        .onFailure(e -> logger.error("Could not obtain settings by module " + moduleName
            + " and config " + configName + ": " + e.getMessage()));
  }

}
