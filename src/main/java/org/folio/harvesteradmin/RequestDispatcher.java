/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.dataaccess.HarvesterApiClient;
import org.folio.harvesteradmin.statics.EntityRootNames;

import java.util.HashMap;
import java.util.Map;

import static org.folio.harvesteradmin.statics.ApiPaths.*;

/**
 * Routes incoming requests to methods of the Harvester API client by mapping request paths to Harvester paths
 *
 * @author ne
 */
public class RequestDispatcher
{
  private static final Map<String, String> harvesterPathByRequestPath = new HashMap<>();
  private static final Map<String, String> rootElementByApi = new HashMap<>();

  static
  {
    harvesterPathByRequestPath.put( THIS_HARVESTABLES_PATH, HARVESTER_HARVESTABLES_PATH );
    harvesterPathByRequestPath.put( THIS_STORAGES_PATH, HARVESTER_STORAGES_PATH );
    harvesterPathByRequestPath.put( THIS_TRANSFORMATIONS_PATH, HARVESTER_TRANSFORMATIONS_PATH );
    harvesterPathByRequestPath.put( THIS_STEPS_PATH, HARVESTER_STEPS_PATH );
    harvesterPathByRequestPath.put( THIS_TRANSFORMATIONS_STEPS_PATH, HARVESTER_TRANSFORMATIONS_STEPS_PATH );

    rootElementByApi.put( HARVESTER_HARVESTABLES_PATH, EntityRootNames.HARVESTABLE_ROOT_PROPERTY );
    rootElementByApi.put( HARVESTER_STORAGES_PATH, EntityRootNames.STORAGE_ROOT_PROPERTY );
    rootElementByApi.put( HARVESTER_TRANSFORMATIONS_PATH, EntityRootNames.TRANSFORMATION_ROOT_PROPERTY );
    rootElementByApi.put( HARVESTER_STEPS_PATH, EntityRootNames.STEP_ROOT_PROPERTY );
    rootElementByApi.put( HARVESTER_TRANSFORMATIONS_STEPS_PATH, EntityRootNames.TRANSFORMATION_STEP_ROOT_PROPERTY );
  }

  private final HarvesterApiClient client;

  public RequestDispatcher( Vertx vertx )
  {
    client = new HarvesterApiClient( vertx );
  }

  public void handleGet( RoutingContext routingContext )
  {
    client.respondWithConfigRecords( routingContext, mapToApiPath( routingContext ) );
  }

  public void handleGetById( RoutingContext routingContext )
  {
    client.respondWithConfigRecordById( routingContext, mapToApiPath( routingContext ) );
  }

  public void handlePut( RoutingContext routingContext )
  {
    client.putConfigRecordAndRespond( routingContext, mapToApiPath( routingContext ),
            mapToNameOfRoot( routingContext ) );
  }

  public void handlePost( RoutingContext routingContext )
  {
    client.postConfigRecordAndRespond( routingContext, mapToApiPath( routingContext ),
            mapToNameOfRoot( routingContext ) );
  }

  public void handleDelete( RoutingContext routingContext )
  {
    client.deleteConfigRecordAndRespond( routingContext, mapToApiPath( routingContext ) );
  }

  /**
   * Get harvester path from request path (after stripping off the ID part of the request path, if any)
   *
   * @param routingContext context to get path and possible ID parameter from
   * @return Harvester API path corresponding to the given request path
   */
  private String mapToApiPath( RoutingContext routingContext )
  {
    return harvesterPathByRequestPath.get(
            routingContext.request().path().replaceAll( "/" + routingContext.pathParam( "id" ) + "$", "" ) );
  }

  /**
   * Get the Harvester's name for the root element of entities from the requested Harvester path. Required for
   * wrapping/unwrapping during data conversions.
   *
   * @param routingContext context to get the request path from
   * @return name of the root element for entities from the Harvester path corresponding to the given request path
   */
  private String mapToNameOfRoot( RoutingContext routingContext )
  {
    return rootElementByApi.get( mapToApiPath( routingContext ) );
  }
}
