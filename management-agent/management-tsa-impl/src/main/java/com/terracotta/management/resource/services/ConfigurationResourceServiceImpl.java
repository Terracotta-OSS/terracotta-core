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

import com.terracotta.management.resource.ConfigEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.ConfigurationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
@Path("/agents/configurations")
public class ConfigurationResourceServiceImpl implements ConfigurationResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationResourceServiceImpl.class);

  private final ConfigurationService configurationService;
  private final RequestValidator requestValidator;

  public ConfigurationResourceServiceImpl() {
    this.configurationService = ServiceLocator.locate(ConfigurationService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }


  @Override
  public Collection<ConfigEntity> geConfigs(UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImpl.geConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Collection<ConfigEntity> configs = new ArrayList<ConfigEntity>();
      configs.addAll(configurationService.getServerConfigs(null));
      configs.addAll(configurationService.getClientConfigs(null));
      return configs;
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<ConfigEntity> getClientConfigs(UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImpl.getClientConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String ids = info.getPathSegments().get(2).getMatrixParameters().getFirst("ids");
      Set<String> clientIds = ids == null ? null : new HashSet<String>(Arrays.asList(ids.split(",")));

      return configurationService.getClientConfigs(clientIds);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA client configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<ConfigEntity> getServerConfigs(UriInfo info) {
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
