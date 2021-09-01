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
import org.apache.log4j.Logger;

import java.lang.management.ManagementFactory;

import static org.folio.harvesteradmin.ApiStatics.*;

/**
 * Main point of entry for the harvester-admin service
 *
 * @author ne
 */
@SuppressWarnings( "SpellCheckingInspection" )
public class MainVerticle extends AbstractVerticle
{

  private final Logger logger = Logger.getLogger( "harvester-admin" );

  @Override
  public void start( Promise<Void> promise )
  {
    new Config();
    logger.setLevel( Config.logLevel );
    logger.info(
            "Starting Harvester admin service " + ManagementFactory.getRuntimeMXBean().getName() + " on port " + Config.servicePort + ". Logging at level " + Config.logLevel.toString() );
    AdminRecordsHandlers adminRecordsHandlers = new AdminRecordsHandlers( vertx );

    Router router = Router.router( vertx );
    router.put( "/*" ).handler( BodyHandler.create() ); // Tell vertx we want the whole PUT body in the handler
    router.post( "/*" ).handler( BodyHandler.create() );

    router.get( THIS_HARVESTABLES_PATH ).handler( adminRecordsHandlers::handleGetHarvestables );
    router.get( THIS_HARVESTABLES_ID_PATH ).handler( adminRecordsHandlers::handleGetHarvestableById );
    router.put( THIS_HARVESTABLES_ID_PATH ).handler( adminRecordsHandlers::handlePutHarvestable );
    router.post( THIS_HARVESTABLES_PATH ).handler( adminRecordsHandlers::handlePostHarvestable );
    router.delete( THIS_HARVESTABLES_ID_PATH ).handler( adminRecordsHandlers::handleDeleteHarvestable );

    router.get( THIS_STORAGES_PATH ).handler( adminRecordsHandlers::handleGetStorages );
    router.get( THIS_STORAGES_ID_PATH ).handler( adminRecordsHandlers::handleGetStorageById );
    router.put( THIS_STORAGES_ID_PATH ).handler( adminRecordsHandlers::handlePutStorage );
    router.post( THIS_STORAGES_PATH ).handler( adminRecordsHandlers::handlePostStorage );
    router.delete( THIS_STORAGES_ID_PATH ).handler( adminRecordsHandlers::handleDeleteStorage );

    router.get( THIS_TRANSFORMATIONS_PATH ).handler( adminRecordsHandlers::handleGetTransformations );
    router.get( THIS_TRANSFORMATIONS_ID_PATH ).handler( adminRecordsHandlers::handleGetTransformationById );
    router.put( THIS_TRANSFORMATIONS_ID_PATH ).handler( adminRecordsHandlers::handlePutTransformation );
    router.post( THIS_TRANSFORMATIONS_PATH ).handler( adminRecordsHandlers::handlePostTransformation );
    router.delete( THIS_TRANSFORMATIONS_ID_PATH ).handler( adminRecordsHandlers::handleDeleteTransformation );

    router.get( THIS_STEPS_PATH ).handler( adminRecordsHandlers::handleGetSteps );
    router.get( THIS_STEPS_ID_PATH ).handler( adminRecordsHandlers::handleGetStepById );
    router.put( THIS_STEPS_ID_PATH ).handler( adminRecordsHandlers::handlePutStep );
    router.post( THIS_STEPS_PATH ).handler( adminRecordsHandlers::handlePostStep );
    router.delete( THIS_STEPS_ID_PATH ).handler( adminRecordsHandlers::handleDeleteStep );

    router.get( THIS_TRANSFORMATIONS_STEPS_PATH ).handler( adminRecordsHandlers::handleGetTransformationSteps );
    router.get( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( adminRecordsHandlers::handleGetTransformationStepById );
    router.put( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( adminRecordsHandlers::handlePutTransformationStep );
    router.post( THIS_TRANSFORMATIONS_STEPS_PATH ).handler( adminRecordsHandlers::handlePostTransformationStep );
    router.delete( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( adminRecordsHandlers::handleDeleteTransformationStep );

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

