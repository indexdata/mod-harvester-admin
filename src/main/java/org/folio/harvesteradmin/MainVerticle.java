/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.globalstatics.Config;

import java.lang.management.ManagementFactory;

import static org.folio.harvesteradmin.globalstatics.ApiStatics.*;

/**
 * Main point of entry for the harvester-admin service
 *
 * @author ne
 */
@SuppressWarnings( "SpellCheckingInspection" )
public class MainVerticle extends AbstractVerticle
{

  private final Logger logger = LogManager.getLogger( "harvester-admin" );

  @Override
  public void start( Promise<Void> promise )
  {
    new Config();
    logger.info(
            "Starting Harvester admin service " + ManagementFactory.getRuntimeMXBean().getName() + " on port " + Config.servicePort );
    RequestHandlers requestHandlers = new RequestHandlers( vertx );

    Router router = Router.router( vertx );
    router.put( "/*" ).handler( BodyHandler.create() ); // Tell vertx we want the whole PUT body in the handler
    router.post( "/*" ).handler( BodyHandler.create() );

    router.get( THIS_HARVESTABLES_PATH ).handler( requestHandlers::handleGetHarvestables );
    router.get( THIS_HARVESTABLES_ID_PATH ).handler( requestHandlers::handleGetHarvestableById );
    router.put( THIS_HARVESTABLES_ID_PATH ).handler( requestHandlers::handlePutHarvestable );
    router.post( THIS_HARVESTABLES_PATH ).handler( requestHandlers::handlePostHarvestable );
    router.delete( THIS_HARVESTABLES_ID_PATH ).handler( requestHandlers::handleDeleteHarvestable );

    router.get( THIS_STORAGES_PATH ).handler( requestHandlers::handleGetStorages );
    router.get( THIS_STORAGES_ID_PATH ).handler( requestHandlers::handleGetStorageById );
    router.put( THIS_STORAGES_ID_PATH ).handler( requestHandlers::handlePutStorage );
    router.post( THIS_STORAGES_PATH ).handler( requestHandlers::handlePostStorage );
    router.delete( THIS_STORAGES_ID_PATH ).handler( requestHandlers::handleDeleteStorage );

    router.get( THIS_TRANSFORMATIONS_PATH ).handler( requestHandlers::handleGetTransformations );
    router.get( THIS_TRANSFORMATIONS_ID_PATH ).handler( requestHandlers::handleGetTransformationById );
    router.put( THIS_TRANSFORMATIONS_ID_PATH ).handler( requestHandlers::handlePutTransformation );
    router.post( THIS_TRANSFORMATIONS_PATH ).handler( requestHandlers::handlePostTransformation );
    router.delete( THIS_TRANSFORMATIONS_ID_PATH ).handler( requestHandlers::handleDeleteTransformation );

    router.get( THIS_STEPS_PATH ).handler( requestHandlers::handleGetSteps );
    router.get( THIS_STEPS_ID_PATH ).handler( requestHandlers::handleGetStepById );
    router.put( THIS_STEPS_ID_PATH ).handler( requestHandlers::handlePutStep );
    router.post( THIS_STEPS_PATH ).handler( requestHandlers::handlePostStep );
    router.delete( THIS_STEPS_ID_PATH ).handler( requestHandlers::handleDeleteStep );

    router.get( THIS_TRANSFORMATIONS_STEPS_PATH ).handler( requestHandlers::handleGetTransformationSteps );
    router.get( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( requestHandlers::handleGetTransformationStepById );
    router.put( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( requestHandlers::handlePutTransformationStep );
    router.post( THIS_TRANSFORMATIONS_STEPS_PATH ).handler( requestHandlers::handlePostTransformationStep );
    router.delete( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( requestHandlers::handleDeleteTransformationStep );

    vertx.createHttpServer().requestHandler( router ).listen( Config.servicePort, result -> {
      if ( result.succeeded() )
      {
        logger.info( "Succeeded in starting the listener for Harvester admin service" );
        promise.complete();
      }
      else
      {
        logger.error( "Harvester admin service failed: " + result.cause().getMessage() );
        promise.fail( result.cause() );
      }
    } );
  }
}

