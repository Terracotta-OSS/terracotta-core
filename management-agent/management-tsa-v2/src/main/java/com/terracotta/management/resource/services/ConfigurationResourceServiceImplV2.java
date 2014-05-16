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

import com.terracotta.management.resource.ConfigEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.resource.services.validator.TSARequestValidatorV2;
import com.terracotta.management.service.ConfigurationServiceV2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
 * A resource service for getting TSA configs.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/configurations")
public class ConfigurationResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationResourceServiceImplV2.class);

  private final ConfigurationServiceV2 configurationService;
  private final RequestValidator requestValidator;

  public ConfigurationResourceServiceImplV2() {
    this.configurationService = ServiceLocator.locate(ConfigurationServiceV2.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidatorV2.class);
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ConfigEntityV2> getConfigs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImpl.geConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);
      Collection<ConfigEntityV2> configs = new ArrayList<ConfigEntityV2>();
      configs.addAll(configurationService.getServerConfigs(null));
      configs.addAll(configurationService.getClientConfigs(null, productIDs));
      return configs;
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/v2/clients")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ConfigEntityV2> getClientConfigs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImpl.getClientConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String ids = info.getPathSegments().get(2).getMatrixParameters().getFirst("ids");
      Set<String> clientIds = ids == null ? null : new HashSet<String>(Arrays.asList(ids.split(",")));
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);

      return configurationService.getClientConfigs(clientIds, productIDs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA client configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/v2/servers")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ConfigEntityV2> getServerConfigs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImpl.getServerConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      return configurationService.getServerConfigs(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA server configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
