/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.TopologyEntity;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.OperatorEventsService;
import com.terracotta.management.service.TopologyService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
@Path("/agents/topologies")
public class TopologyResourceServiceImpl {

  private static final Logger LOG = LoggerFactory.getLogger(TopologyResourceServiceImpl.class);

  private final TopologyService topologyService;
  private final RequestValidator requestValidator;
  private final OperatorEventsService operatorEventsService;

  public TopologyResourceServiceImpl() {
    this.topologyService = ServiceLocator.locate(TopologyService.class);
    this.operatorEventsService = ServiceLocator.locate(OperatorEventsService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }

  /**
   * Get a {@code Collection} of {@link TopologyEntity} objects representing the entire cluster topology provided by the
   * associated monitorable entity's agent given the request path.
   *
   * @return a collection of {@link TopologyEntity} objects.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<TopologyEntity> getTopologies(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyServiceImpl.getTopologies: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);

      TopologyEntity result = new TopologyEntity();
      result.setVersion(this.getClass().getPackage().getImplementationVersion());
      result.getServerGroupEntities().addAll(topologyService.getServerGroups(null));
      result.getClientEntities().addAll(topologyService.getClients(productIDs));
      result.setUnreadOperatorEventCount(operatorEventsService.getUnreadCount(null));
      return Collections.singleton(result);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA topologies", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * Get a {@code Collection} of {@link TopologyEntity} objects representing the TSA topology provided by the associated
   * monitorable entity's agent given the request path.
   *
   * @return a collection of {@link TopologyEntity} objects.
   */
  @GET
  @Path("/servers")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<TopologyEntity> getServerTopologies(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyServiceImpl.getServerTopologies: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      TopologyEntity result = new TopologyEntity();
      result.setVersion(this.getClass().getPackage().getImplementationVersion());
      result.getServerGroupEntities().addAll(topologyService.getServerGroups(serverNames));
      result.setUnreadOperatorEventCount(operatorEventsService.getUnreadCount(serverNames));
      return Collections.singleton(result);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA servers topologies", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * Get a {@code Collection} of {@link ClientEntity} objects representing the connected clients provided by the
   * associated monitorable entity's agent given the request path.
   *
   * @return a collection of {@link ClientEntity} objects.
   */
  @GET
  @Path("/clients")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<TopologyEntity> getConnectedClients(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyServiceImpl.getConnectedClients: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);

      TopologyEntity result = new TopologyEntity();
      result.setVersion(this.getClass().getPackage().getImplementationVersion());
      result.getClientEntities().addAll(topologyService.getClients(productIDs));
      return Collections.singleton(result);
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
  public Collection<TopologyEntity> getUnreadOperatorEventCount(@Context UriInfo info) {
    LOG.debug(String.format("Invoking TopologyServiceImpl.getUnreadOperatorEventCount: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("serverNames");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      TopologyEntity result = new TopologyEntity();
      result.setVersion(this.getClass().getPackage().getImplementationVersion());
      result.setUnreadOperatorEventCount(operatorEventsService.getUnreadCount(serverNames));
      return Collections.singleton(result);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA unread operator events count", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
