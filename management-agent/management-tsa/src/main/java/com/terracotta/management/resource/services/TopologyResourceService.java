/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.TopologyEntity;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA topologies.
 *
 * @author Ludovic Orban
 */
public interface TopologyResourceService {

  /**
   * Get a {@code Collection} of {@link TopologyEntity} objects representing the entire cluster
   * topology provided by the associated monitorable entity's agent given the request path.
   *
   * @return a collection of {@link TopologyEntity} objects.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<TopologyEntity> getTopologies(@Context UriInfo info);

  /**
   * Get a {@code Collection} of {@link TopologyEntity} objects representing the
   * TSA topology provided by the associated monitorable entity's agent given the request path.
   *
   * @return a collection of {@link TopologyEntity} objects.
   */
  @GET
  @Path("/servers")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<TopologyEntity> getServerTopologies(@Context UriInfo info);

  /**
   * Get a {@code Collection} of {@link ClientEntity} objects representing the connected clients
   * provided by the associated monitorable entity's agent given the request path.
   *
   * @return a collection of {@link ClientEntity} objects.
   */
  @GET
  @Path("/clients")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<TopologyEntity> getConnectedClients(@Context UriInfo info);

  /**
   * Get a {@code Map} of String/Integers (event type / count) representing the unread operator events.
   *
   * @return a map of String/Integers.
   */
  @GET
  @Path("/unreadOperatorEventCount")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<TopologyEntity> getUnreadOperatorEventCount(@Context UriInfo info);

}
