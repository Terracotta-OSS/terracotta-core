/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.TopologyEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.TopologyService;

import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
@Path("/agents/topologies")
public class TopologyResourceServiceImpl implements TopologyResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(TopologyResourceServiceImpl.class);

  private final TopologyService topologyService;
  private final RequestValidator requestValidator;

  public TopologyResourceServiceImpl() {
    this.topologyService = ServiceLocator.locate(TopologyService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }

  @Override
  public Collection<TopologyEntity> getTopologies(UriInfo info) {
    LOG.info(String.format("Invoking TopologyServiceImpl.getTopologies: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      TopologyEntity result = new TopologyEntity();
      result.getServerGroupEntities().addAll(topologyService.getTopology());
      result.getClientEntities().addAll(topologyService.getClients());
      return Collections.singleton(result);
    } catch (ServiceExecutionException see) {
      LOG.error("Failed to get TSA topologies.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(see.getCause().getMessage()).build());
    }
  }

  @Override
  public Collection<TopologyEntity> getServerTopologies(UriInfo info) {
    LOG.info(String.format("Invoking TopologyServiceImpl.getServerTopologies: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      TopologyEntity result = new TopologyEntity();
      result.getServerGroupEntities().addAll(topologyService.getTopology());
      return Collections.singleton(result);
    } catch (ServiceExecutionException see) {
      LOG.error("Failed to get TSA servers topologies.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(see.getCause().getMessage()).build());
    }
  }

  @Override
  public Collection<TopologyEntity> getConnectedClients(UriInfo info) {
    LOG.info(String.format("Invoking TopologyServiceImpl.getConnectedClients: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      TopologyEntity result = new TopologyEntity();
      result.getClientEntities().addAll(topologyService.getClients());
      return Collections.singleton(result);
    } catch (ServiceExecutionException see) {
      LOG.error("Failed to get TSA clients topologies.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(see.getCause().getMessage()).build());
    }
  }

}
