package org.folio.harvesteradmin.foliodata;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class ConfigurationsClient {
    public static final String CONFIGURATIONS_PATH = "/configurations/entries";
    public static final String RECORDS = "configs";

    protected static final Logger logger =
            LogManager.getLogger(ConfigurationsClient.class);

    public static Future<String> getStringValue (RoutingContext routingContext, String moduleName, String configName) {
        String query = "module==" + moduleName + " and configName==" + configName + " and enabled=true";
        Promise<String> promise = Promise.promise();
        Folio.okapiClient(routingContext).get(CONFIGURATIONS_PATH +
                        "?query=(" + URLEncoder.encode(query, StandardCharsets.UTF_8) +")")
                .onSuccess(response -> {
                    JsonObject json = new JsonObject(response);
                    JsonArray entries = json.getJsonArray(RECORDS);
                    if (entries.isEmpty()) {
                        promise.complete(null);

                    } else {
                        JsonObject entry = entries.getJsonObject(0);
                        promise.complete(entry.getString("value"));
                    }
                }).onFailure(response -> {
                    logger.info("Could not obtain settings by module " + moduleName + " and config " + configName + ": " + response.getMessage());
                    promise.complete(null);
                });
        return promise.future();
    }

}
