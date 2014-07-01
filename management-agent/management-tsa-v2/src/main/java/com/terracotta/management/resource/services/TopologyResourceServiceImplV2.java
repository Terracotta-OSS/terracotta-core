/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.TopologyEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.TopologyServiceV2;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA topologies.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/topologies")
public class TopologyResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(TopologyResourceServiceImplV2.class);

  private final TopologyServiceV2 topologyService;
  private final RequestValidator requestValidator;

  public TopologyResourceServiceImplV2() {
    this.topologyService = ServiceLocator.locate(TopologyServiceV2.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  /**
   * Get a {@code Collection} of {@link TopologyEntityV2} objects representing the entire cluster topology provided by the
   * associated monitorable entity's agent given the request path.
   *
   * @return a collection of {@link TopologyEntityV2} objects.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<TopologyEntityV2> getTopologies(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyResourceServiceImplV2.getTopologies: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);
      return topologyService.getTopologies(productIDs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA topologies", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * Get a {@code Collection} of {@link TopologyEntityV2} objects representing the TSA topology provided by the associated
   * monitorable entity's agent given the request path.
   *
   * @return a collection of {@link TopologyEntityV2} objects.
   */
  @GET
  @Path("/servers")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<TopologyEntityV2> getServerTopologies(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyResourceServiceImplV2.getServerTopologies: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "names");
      return topologyService.getServerTopologies(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA servers topologies", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * Get a {@code Collection} of {@link TopologyEntityV2} objects representing the connected clients provided by the
   * associated monitorable entity's agent given the request path.
   *
   * @return a collection of {@link TopologyEntityV2} objects.
   */
  @GET
  @Path("/clients")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<TopologyEntityV2> getConnectedClients(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyResourceServiceImplV2.getConnectedClients: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);
      Set<String> clientIds = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "ids");
      return topologyService.getConnectedClients(productIDs, clientIds);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA clients topologies", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * Get a {@code Map} of String/Integers (event type / count) representing the unread operator events.
   *
   * @return a map of String/Integers.
   */
  @GET
  @Path("/unreadOperatorEventCount")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<TopologyEntityV2> getUnreadOperatorEventCount(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyResourceServiceImplV2.getUnreadOperatorEventCount: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "serverNames");
      return topologyService.getUnreadOperatorEventCount(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA unread operator events count", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
