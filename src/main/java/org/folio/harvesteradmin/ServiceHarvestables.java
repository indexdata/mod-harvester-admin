/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 *
 * @author ne
 */
public class ServiceHarvestables {
  public final static String HARVESTABLES_PATH = "/harvestables";
  
   private final Logger logger = LoggerFactory.getLogger("harvester-admin");

  public void handleGetHarvestables(RoutingContext routingCtx) {
    logger.info("handling /harvestables");
    String contentType = routingCtx.request().getHeader("Content-Type");
    if (contentType != null && !contentType.startsWith("application/json")) {
      responseError(routingCtx, 400, "Only accepts Content-Type application/json, was: "+ contentType);
    } else {
      responseJson(routingCtx,200).end("{ \"response\": \"Hello World!\" }");
    }
  }

}
