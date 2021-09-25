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
import org.folio.harvesteradmin.statics.Config;

import java.lang.management.ManagementFactory;

import static org.folio.harvesteradmin.statics.ApiPaths.*;

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
    RequestDispatcher requestDispatcher = new RequestDispatcher( vertx );
    JobLauncher jobLauncher = new JobLauncher( vertx );
    ScriptHandler scriptHandler = new ScriptHandler( vertx );

    Router router = Router.router( vertx );
    router.put( "/*" ).handler( BodyHandler.create() ); // Tell vertx we want the whole PUT body in the handler
    router.post( "/*" ).handler( BodyHandler.create() );

    router.get( THIS_HARVESTABLES_PATH ).handler( requestDispatcher::handleGet );
    router.get( THIS_HARVESTABLES_ID_PATH ).handler( requestDispatcher::handleGetById );
    router.put( THIS_HARVESTABLES_ID_PATH ).handler( requestDispatcher::handlePut );
    router.post( THIS_HARVESTABLES_PATH ).handler( requestDispatcher::handlePost );
    router.delete( THIS_HARVESTABLES_ID_PATH ).handler( requestDispatcher::handleDeleteById );
    router.delete( THIS_HARVESTABLES_PATH ).handler( requestDispatcher::handleDelete );

    router.get( THIS_STORAGES_PATH ).handler( requestDispatcher::handleGet );
    router.get( THIS_STORAGES_ID_PATH ).handler( requestDispatcher::handleGetById );
    router.put( THIS_STORAGES_ID_PATH ).handler( requestDispatcher::handlePut );
    router.post( THIS_STORAGES_PATH ).handler( requestDispatcher::handlePost );
    router.delete( THIS_STORAGES_ID_PATH ).handler( requestDispatcher::handleDeleteById );
    router.delete( THIS_STORAGES_PATH ).handler( requestDispatcher::handleDelete );

    router.get( THIS_TRANSFORMATIONS_PATH ).handler( requestDispatcher::handleGet );
    router.get( THIS_TRANSFORMATIONS_ID_PATH ).handler( requestDispatcher::handleGetById );
    router.put( THIS_TRANSFORMATIONS_ID_PATH ).handler( requestDispatcher::handlePut );
    router.post( THIS_TRANSFORMATIONS_PATH ).handler( requestDispatcher::handlePost );
    router.delete( THIS_TRANSFORMATIONS_ID_PATH ).handler( requestDispatcher::handleDeleteById );
    router.delete( THIS_TRANSFORMATIONS_PATH ).handler( requestDispatcher::handleDelete );

    router.get( THIS_STEPS_PATH ).handler( requestDispatcher::handleGet );
    router.get( THIS_STEPS_ID_PATH ).handler( requestDispatcher::handleGetById );
    router.put( THIS_STEPS_ID_PATH ).handler( requestDispatcher::handlePut );
    router.post( THIS_STEPS_PATH ).handler( requestDispatcher::handlePost );
    router.delete( THIS_STEPS_ID_PATH ).handler( requestDispatcher::handleDeleteById );
    router.delete( THIS_STEPS_PATH ).handler( requestDispatcher::handleDelete );
    router.get( THIS_STEPS_ID_SCRIPT_PATH ).handler( scriptHandler::handleGetScript );
    router.put( THIS_STEPS_ID_SCRIPT_PATH ).handler( scriptHandler::handlePutScript );

    router.get( THIS_TRANSFORMATIONS_STEPS_PATH ).handler( requestDispatcher::handleGet );
    router.get( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( requestDispatcher::handleGetById );
    router.put( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( requestDispatcher::handlePut );
    router.post( THIS_TRANSFORMATIONS_STEPS_PATH ).handler( requestDispatcher::handlePost );
    router.delete( THIS_TRANSFORMATIONS_STEPS_ID_PATH ).handler( requestDispatcher::handleDeleteById );
    router.delete( THIS_TRANSFORMATIONS_STEPS_PATH ).handler( requestDispatcher::handleDelete );

    router.put( THIS_RUN_JOB_PATH ).handler( jobLauncher::startJob );

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

