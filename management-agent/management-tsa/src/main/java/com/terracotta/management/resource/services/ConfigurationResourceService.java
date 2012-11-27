/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.ConfigEntity;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for getting TSA configs.
 *
 * @author Ludovic Orban
 */
public interface ConfigurationResourceService {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<ConfigEntity> geConfigs(@Context UriInfo info);

  @GET
  @Path("/clients")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<ConfigEntity> getClientConfigs(@Context UriInfo info);

  @GET
  @Path("/servers")
  @Produces(MediaType.APPLICATION_JSON)
  Collection<ConfigEntity> getServerConfigs(@Context UriInfo info);

}
