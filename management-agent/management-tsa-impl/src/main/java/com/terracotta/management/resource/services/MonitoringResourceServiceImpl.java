/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
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
import javax.ws.rs.WebApplicationException;
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
    LOG.info(String.format("Invoking MonitoringResourceServiceImpl.getServerStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));
      if (serverNames == null) {
        serverNames = monitoringService.getAllServerNames();
      }

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      List<String> attrs = qParams.get(ATTR_QUERY_KEY);
      Set<String> tsaAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

      Collection<StatisticsEntity> statistics = new ArrayList<StatisticsEntity>();
      for (String serverName : serverNames) {
        StatisticsEntity entity = monitoringService.getServerStatistics(serverName, tsaAttrs);
        if (entity != null) {
          statistics.add(entity);
        }
      }

      return statistics;
    } catch (ServiceExecutionException see) {
      LOG.error("Failed to get TSA statistics.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Failed to get TSA statistics: " + see.getCause().getClass().getName() + ": " + see.getCause()
                  .getMessage()).build());
    }
  }

  @Override
  public Collection<StatisticsEntity> getDgcStatistics(UriInfo info) {
    LOG.info(String.format("Invoking MonitoringResourceServiceImpl.getDgcStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return monitoringService.getDgcStatistics();
    } catch (ServiceExecutionException see) {
      LOG.error("Failed to get TSA statistics.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Failed to get TSA statistics: " + see.getCause().getClass().getName() + ": " + see.getCause()
                  .getMessage()).build());
    }
  }

  @Override
  public Collection<StatisticsEntity> getClientStatistics(UriInfo info) {
    LOG.info(String.format("Invoking MonitoringResourceServiceImpl.getClientStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String ids = info.getPathSegments().get(2).getMatrixParameters().getFirst("ids");
      Set<String> clientIds = ids == null ? null : new HashSet<String>(Arrays.asList(ids.split(",")));
      if (clientIds == null) {
        clientIds = monitoringService.getAllClientIds();
      }

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      List<String> attrs = qParams.get(ATTR_QUERY_KEY);
      Set<String> tsaAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

      Collection<StatisticsEntity> statistics = new ArrayList<StatisticsEntity>();
      for (String clientId : clientIds) {
        StatisticsEntity entity = monitoringService.getClientStatistics(clientId, tsaAttrs);
        if (entity != null) {
          statistics.add(entity);
        }
      }

      return statistics;
    } catch (ServiceExecutionException see) {
      LOG.error("Failed to get TSA statistics.", see.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity("Failed to get TSA statistics: " + see.getCause().getClass().getName() + ": " + see.getCause().getMessage()).build());
    }
  }

}
