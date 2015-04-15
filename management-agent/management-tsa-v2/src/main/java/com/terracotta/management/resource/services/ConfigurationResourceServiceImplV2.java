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
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.ConfigEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.ConfigurationServiceV2;

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
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<ConfigEntityV2> getConfigs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImplV2.geConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);
      ResponseEntityV2<ConfigEntityV2> responseEntityV2 = new ResponseEntityV2<ConfigEntityV2>();
      responseEntityV2.getEntities().addAll(configurationService.getServerConfigs(null).getEntities());
      responseEntityV2.getEntities().addAll(configurationService.getClientConfigs(null, productIDs).getEntities());
      return responseEntityV2;
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/clients")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<ConfigEntityV2> getClientConfigs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImplV2.getClientConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> clientIds = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "ids");
      Set<String> productIDs = UriInfoUtils.extractProductIds(info);

      return configurationService.getClientConfigs(clientIds, productIDs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA client configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @GET
  @Path("/servers")
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<ConfigEntityV2> getServerConfigs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ConfigurationResourceServiceImplV2.getServerConfigs: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "names");

      return configurationService.getServerConfigs(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA server configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
