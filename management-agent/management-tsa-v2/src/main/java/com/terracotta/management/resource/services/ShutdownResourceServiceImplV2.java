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

import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.ShutdownServiceV2;

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
@Path("/v2/agents/shutdown")
public class ShutdownResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(ShutdownResourceServiceImplV2.class);

  private final ShutdownServiceV2 shutdownService;
  private final RequestValidator requestValidator;

  public ShutdownResourceServiceImplV2() {
    this.shutdownService = ServiceLocator.locate(ShutdownServiceV2.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public boolean shutdown(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ShutdownResourceServiceImplV2.shutdown: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "names");

      shutdownService.shutdown(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to shutdown TSA", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
    
    return true;
  }

}
