/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.DiagnosticsService;

import java.util.Collection;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
@Path("/agents/diagnostics")
public class DiagnosticsResourceServiceImpl implements DiagnosticsResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(DiagnosticsResourceServiceImpl.class);

  private final DiagnosticsService diagnosticsService;
  private final RequestValidator requestValidator;

  public DiagnosticsResourceServiceImpl() {
    this.diagnosticsService = ServiceLocator.locate(DiagnosticsService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }


  @Override
  public Collection<ThreadDumpEntity> clusterThreadDump(UriInfo info) {
    LOG.info(String.format("Invoking DiagnosticsResourceServiceImpl.clusterThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return diagnosticsService.getClusterThreadDump();
    } catch (ServiceExecutionException see) {
      LOG.error("Failed to perform TSA diagnostics.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Failed to perform TSA diagnostics: " + see.getCause().getClass().getName() + ": " + see.getCause()
                  .getMessage()).build());
    }
  }

  @Override
  public boolean runDgc(UriInfo info) {
    LOG.info(String.format("Invoking DiagnosticsResourceServiceImpl.runDgc: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return diagnosticsService.runDgc();
    } catch (ServiceExecutionException see) {
      LOG.error("Failed to perform TSA diagnostics.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Failed to perform TSA diagnostics: " + see.getCause().getClass().getName() + ": " + see.getCause()
                  .getMessage()).build());
    }
  }
}
