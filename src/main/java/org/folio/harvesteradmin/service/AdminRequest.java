package org.folio.harvesteradmin.service;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage;
import org.folio.harvesteradmin.moduledata.database.ModuleStorageAccess;

import java.util.HashMap;
import java.util.Map;

import static org.folio.harvesteradmin.legacydata.statics.ApiPaths.harvesterPathByRequestPath;
import static org.folio.harvesteradmin.legacydata.statics.RequestParameters.supportedGetRequestParameters;

public abstract class AdminRequest {

    protected Vertx vertx;
    protected RoutingContext routingContext;
    protected String tenant;
    protected HttpServerRequest request;
    protected String requestPath;

    public Vertx vertx() {
        return vertx;
    }

    public String tenant () {
        return tenant;
    }

    public abstract JsonObject bodyAsJson();

    public abstract String bodyAsString();

    public ModuleStorageAccess moduleStorageAccess() {
        return new ModuleStorageAccess(vertx, tenant);
    }

    public LegacyHarvesterStorage legacyHarvesterAccess() {
        return new LegacyHarvesterStorage(vertx, tenant);
    }

    public String harvesterPathFromRequestPath() {
        return harvesterPathByRequestPath.get(requestPath.replaceAll("/" + routingContext.pathParam("id") + "$", ""));
    }

    public abstract String queryParam(String paramName);

    public abstract String pathParam(String paramName);

    public String absoluteURI () {
        return request.absoluteURI();
    }

    public String path() {
        return requestPath;
    }

    public RoutingContext routingContext() {
        return routingContext;
    }

    public Map<String, String> supportedLegacyGetParameters() {
        Map<String, String> requestParameterMap = new HashMap<>();
        for (String param : supportedGetRequestParameters) {
            if (queryParam(param) != null) {
                requestParameterMap.put(param, queryParam(param));
            }
        }
        return requestParameterMap;
    }

    public String requestParam(String paramName) {
        return request.getParam(paramName);
    }

}
