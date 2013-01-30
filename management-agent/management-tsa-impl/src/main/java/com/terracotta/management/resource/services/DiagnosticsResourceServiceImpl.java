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

import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.DiagnosticsService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.clusterThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return diagnosticsService.getClusterThreadDump();
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<ThreadDumpEntity> serversThreadDump(UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.serversThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(3).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      return diagnosticsService.getServersThreadDump(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<ThreadDumpEntity> clientsThreadDump(UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.clientsThreadDump: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String ids = info.getPathSegments().get(3).getMatrixParameters().getFirst("ids");
      Set<String> clientIds = ids == null ? null : new HashSet<String>(Arrays.asList(ids.split(",")));

      return diagnosticsService.getClientsThreadDump(clientIds);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public boolean runDgc(UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.runDgc: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return diagnosticsService.runDgc();
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<TopologyReloadStatusEntity> reloadConfiguration(UriInfo info) {
    LOG.debug(String.format("Invoking DiagnosticsResourceServiceImpl.reloadConfiguration: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return diagnosticsService.reloadConfiguration();
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA diagnostics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
