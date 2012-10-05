/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.AgentEntity;

import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.TopologyEntity;
import com.terracotta.management.service.TopologyService;

import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
@Path("/agents/topologies")
public class TopologyResourceServiceImpl implements TopologyResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(TopologyResourceServiceImpl.class);

  private final TopologyService topologyService;

  public TopologyResourceServiceImpl() {
    this.topologyService = ServiceLocator.locate(TopologyService.class);
  }

  @Override
  public Collection<TopologyEntity> getServerTopologies(UriInfo info) {
    LOG.info(String.format("Invoking TopologyServiceImpl.getServerTopologies: %s", info.getRequestUri()));

    //TODO: this should go in a validator
    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");
    if (ids != null && !ids.equals(AgentEntity.EMBEDDED_AGENT_ID)) {
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity("Invalid agent ID : " + ids).build());
    }

    try {
      return Collections.singleton(topologyService.getTopology());
    } catch (ServiceExecutionException e) {
      LOG.error("Failed to get TSA topologies.", e.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(e.getCause().getMessage()).build());
    }
  }

  @Override
  public Collection<ClientEntity> getConnectedClients(@Context UriInfo info) {
    LOG.info(String.format("Invoking TopologyServiceImpl.getConnectedClients: %s", info.getRequestUri()));

    //TODO: this should go in a validator
    String ids = info.getPathSegments().get(0).getMatrixParameters().getFirst("ids");
    if (ids != null && !ids.equals(AgentEntity.EMBEDDED_AGENT_ID)) {
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity("Invalid agent ID : " + ids).build());
    }

    try {
      return topologyService.getClients();
    } catch (ServiceExecutionException e) {
      LOG.error("Failed to get TSA clients.", e.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(e.getCause().getMessage()).build());
    }
  }

}
