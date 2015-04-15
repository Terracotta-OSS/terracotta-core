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

import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.ShutdownService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for performing TSA shutdown.
 * 
 * @author Ludovic Orban
 */
@Path("/agents/shutdown")
public class ShutdownResourceServiceImpl {

  private static final Logger LOG = LoggerFactory.getLogger(ShutdownResourceServiceImpl.class);

  private final ShutdownService shutdownService;
  private final RequestValidator requestValidator;

  public ShutdownResourceServiceImpl() {
    this.shutdownService = ServiceLocator.locate(ShutdownService.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public boolean shutdown(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ShutdownResourceServiceImpl.shutdown: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      shutdownService.shutdown(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to shutdown TSA", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
    
    return true;
  }

}
