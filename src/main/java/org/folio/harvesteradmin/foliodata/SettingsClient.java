package org.folio.harvesteradmin.foliodata;

import static org.folio.util.StringUtil.cqlEncode;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.util.PercentCodec;

/**
 * Run GET /settings/entries HTTP request to get a setting from mod-settings.
 */
public class SettingsClient {
  public static final String SETTINGS_PATH = "/settings/entries";
  public static final String RECORDS = "items";

  protected static final Logger logger =
      LogManager.getLogger(SettingsClient.class);

  /**
   * Get the settings value from mod-settings for the scope and key.
   */
  public static Future<String> getStringValue(RoutingContext routingContext,
      String scope, String key) {

    var cql = "scope==" + cqlEncode(scope) + " and key==" + cqlEncode(key);
    return Folio.okapiClient(routingContext)
        .get(SETTINGS_PATH + "?query=" + PercentCodec.encode(cql))
        .map(response ->
            new JsonObject(response).getJsonArray(RECORDS).getJsonObject(0).getString("value"))
        .onFailure(e ->
            logger.error("Could not obtain settings by scope " + scope
                + " and key " + key + ": " + e.getMessage()));
  }

}
