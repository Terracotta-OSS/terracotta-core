/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.StatisticsEntity;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA runtime statistics.
 *
 * @author Ludovic Orban
 */
public interface MonitoringResourceService {

  public final static String ATTR_QUERY_KEY = "show";

  /**
   * Get a {@code Collection} of {@link StatisticsEntity} objects representing the
   * server(s) statistics provided by the associated monitorable entity's agent given the request path.
   *
   * @return a a collection of {@link StatisticsEntity} objects.
   */
  @GET
  @Path("/servers")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<StatisticsEntity> getServerStatistics(@Context UriInfo info);

  /**
   * Get a {@code Collection} of {@link StatisticsEntity} objects representing the
   * DGC statistics of each iteration provided by the associated monitorable entity's agent given the request path.
   *
   * @return a a collection of {@link StatisticsEntity} objects.
   */
  @GET
  @Path("/dgc")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<StatisticsEntity> getDgcStatistics(@Context UriInfo info);

  /**
   * Get a {@code Collection} of {@link StatisticsEntity} objects representing the
   * client(s) statistics provided by the associated monitorable entity's agent given the request path.
   *
   * @return a a collection of {@link StatisticsEntity} objects.
   */
  @GET
  @Path("/clients")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<StatisticsEntity> getClientStatistics(@Context UriInfo info);

}
