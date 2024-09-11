package org.folio.harvesteradmin.foliodata;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Promise;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ConfigurationsClient {
    private static final String CONFIGURATIONS_PATH = "/configurations/entries";
    private static final String RECORDS = "configs";
    public static final String MODULE_HARVESTER_ADMIN = "HARVESTER_ADMIN";
    public static final String CONFIG_NAME_PURGE_LOGS_AFTER = "PURGE_LOGS_AFTER";
    public static Future<JsonArray> getEntries(RoutingContext routingContext, String moduleName, String configName) {

        String query = "module==" + moduleName + " and configName==" + configName + " and enabled==true";
        return Folio.okapiClient(routingContext).get(CONFIGURATIONS_PATH +
                        "?query=(" + URLEncoder.encode(query, StandardCharsets.UTF_8) +")")
                .map(result -> new JsonObject(result).getJsonArray(RECORDS))
                .recover(e -> Future.failedFuture(e.getMessage()));
    }

    public static Future<String> getStringValue (RoutingContext routingContext, String moduleName, String configName) {
        String query = "module=" + moduleName + " and configName=" + configName + " and enabled=true";
        Promise<String> promise = Promise.promise();
        Folio.okapiClient(routingContext).get(CONFIGURATIONS_PATH +
                        "?query=(" + URLEncoder.encode(query, StandardCharsets.UTF_8) +")")
                .onComplete(response -> {
                    JsonObject json = new JsonObject(response.result());
                    JsonArray entries = json.getJsonArray(RECORDS);
                    if (entries.isEmpty()) {
                        promise.complete(null);

                    } else {
                        JsonObject entry = entries.getJsonObject(0);
                        promise.complete(entry.getString("value"));
                    }
                });
        return promise.future();
    }

}
