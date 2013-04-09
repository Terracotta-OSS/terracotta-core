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

import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.MonitoringService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
@Path("/agents/statistics")
public class MonitoringResourceServiceImpl implements MonitoringResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(MonitoringResourceServiceImpl.class);

  private final MonitoringService monitoringService;
  private final RequestValidator requestValidator;

  public MonitoringResourceServiceImpl() {
    this.monitoringService = ServiceLocator.locate(MonitoringService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }

  @Override
  public Collection<StatisticsEntity> getServerStatistics(UriInfo info) {
    LOG.debug(String.format("Invoking MonitoringResourceServiceImpl.getServerStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      List<String> attrs = qParams.get(ATTR_QUERY_KEY);
      Set<String> tsaAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

      return monitoringService.getServerStatistics(serverNames, tsaAttrs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA statistics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<StatisticsEntity> getDgcStatistics(UriInfo info) {
    LOG.debug(String.format("Invoking MonitoringResourceServiceImpl.getDgcStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("serverNames");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      return monitoringService.getDgcStatistics(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA statistics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<StatisticsEntity> getClientStatistics(UriInfo info) {
    LOG.debug(String.format("Invoking MonitoringResourceServiceImpl.getClientStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String ids = info.getPathSegments().get(2).getMatrixParameters().getFirst("ids");
      Set<String> clientIds = ids == null ? null : new HashSet<String>(Arrays.asList(ids.split(",")));

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      List<String> attrs = qParams.get(ATTR_QUERY_KEY);
      Set<String> tsaAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

      return monitoringService.getClientStatistics(clientIds, tsaAttrs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA statistics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
