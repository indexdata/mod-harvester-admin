package org.folio.harvesteradmin.foliodata;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.OkapiClient;

import java.nio.charset.StandardCharsets;

public class InventoryUpdateClient {
    private static final String INVENTORY_UPSERT_PATH = "/inventory-batch-upsert-hrid";
    private final OkapiClient okapiClient;
    private static InventoryUpdateClient inventoryUpdateClient;
    public static final Logger logger = LogManager.getLogger("InventoryUpdateClient");


    private InventoryUpdateClient (RoutingContext routingContext) {
        okapiClient = Folio.okapiClient(routingContext);
    }

    public static InventoryUpdateClient getClient (RoutingContext routingContext) {
        if (inventoryUpdateClient == null) {
            inventoryUpdateClient = new InventoryUpdateClient(routingContext);
        }
        return inventoryUpdateClient;
    }

    public Future<JsonObject> inventoryUpsert (JsonObject recordSets) {
        Buffer records = Buffer.buffer(recordSets.encode().getBytes(StandardCharsets.UTF_8));
        okapiClient.disableInfoLog();
        return okapiClient
                .request(HttpMethod.PUT,INVENTORY_UPSERT_PATH, records)
                .map(JsonObject::new)
                .compose(responseJson -> {
                    if (okapiClient.getStatusCode() == 207) {
                        logger.error(responseJson.getJsonArray("errors").getJsonObject(0).getString("shortMessage"));
                    }
                    return Future.succeededFuture(responseJson);
                })
                .onFailure(e -> logger.error("Could not upsert batch: " + e.getMessage()));
    }
}
