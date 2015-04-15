/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.ConfigEntity;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.ConfigurationService;

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
@Path("/agents/configurations")
public class ConfigurationResourceServiceImpl {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationResourceServiceImpl.class);

  private final ConfigurationService configurationService;
  private final RequestValidator requestValidator;

  public ConfigurationResourceServiceImpl() {
    this.configurationService = ServiceLocator.locate(ConfigurationService.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ConfigEntity> getConfigs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImpl.geConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);
      Collection<ConfigEntity> configs = new ArrayList<ConfigEntity>();
      configs.addAll(configurationService.getServerConfigs(null));
      configs.addAll(configurationService.getClientConfigs(null, productIDs));
      return configs;
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/clients")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ConfigEntity> getClientConfigs(@Context UriInfo info) {
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
  @Path("/servers")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ConfigEntity> getServerConfigs(@Context UriInfo info) {
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
