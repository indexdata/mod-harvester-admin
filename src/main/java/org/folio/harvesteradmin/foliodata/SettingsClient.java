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

public class SettingsClient {
    public static final String SETTINGS_PATH = "/settings/entries";
    public static final String RECORDS = "items";

    protected static final Logger logger =
            LogManager.getLogger(SettingsClient.class);

    public static Future<String> getStringValue (RoutingContext routingContext, String scope, String key) {
        String query = "scope==" + scope + " and key==" + key;
        Promise<String> promise = Promise.promise();
        Folio.okapiClient(routingContext).get(SETTINGS_PATH +
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
                    logger.error("Could not obtain settings by scope " + scope + " and key " + key + ": " + response.getMessage());
                    promise.complete(null);
                });
        return promise.future();
    }

}