/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.harvesteradmin;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.folio.harvesteradmin.dataaccess.HarvesterApiClient;

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

  static
  {
    harvesterPathByRequestPath.put( THIS_HARVESTABLES_PATH, HARVESTER_HARVESTABLES_PATH );
    harvesterPathByRequestPath.put( THIS_STORAGES_PATH, HARVESTER_STORAGES_PATH );
    harvesterPathByRequestPath.put( THIS_TRANSFORMATIONS_PATH, HARVESTER_TRANSFORMATIONS_PATH );
    harvesterPathByRequestPath.put( THIS_STEPS_PATH, HARVESTER_STEPS_PATH );
    harvesterPathByRequestPath.put( THIS_TRANSFORMATIONS_STEPS_PATH, HARVESTER_TRANSFORMATIONS_STEPS_PATH );

  }

  private final HarvesterApiClient client;

  public RequestDispatcher( Vertx vertx )
  {
    client = new HarvesterApiClient( vertx );
  }

  public void handleGet( RoutingContext routingContext )
  {
    client.respondWithConfigRecords( routingContext, mapToHarvesterPath( routingContext ) );
  }

  public void handleGetById( RoutingContext routingContext )
  {
    client.respondWithConfigRecordById( routingContext, mapToHarvesterPath( routingContext ) );
  }

  public void handlePut( RoutingContext routingContext )
  {
    client.putConfigRecordAndRespond( routingContext, mapToHarvesterPath( routingContext ) );
  }

  public void handlePost( RoutingContext routingContext )
  {
    client.postConfigRecordAndRespond( routingContext, mapToHarvesterPath( routingContext ) );
  }

  public void handleDeleteById( RoutingContext routingContext )
  {
    client.deleteConfigRecordAndRespond( routingContext, mapToHarvesterPath( routingContext ) );
  }

  public void handleDelete( RoutingContext routingContext )
  {
    client.deleteConfigRecordsAndRespond( routingContext, mapToHarvesterPath( routingContext ) );
  }

  /**
   * Get harvester path from request path (after stripping off the ID part of the request path, if any)
   *
   * @param routingContext context to get path and possible ID parameter from
   * @return Harvester API path corresponding to the given request path
   */
  private String mapToHarvesterPath( RoutingContext routingContext )
  {
    return harvesterPathByRequestPath.get(
            routingContext.request().path().replaceAll( "/" + routingContext.pathParam( "id" ) + "$", "" ) );
  }

}
