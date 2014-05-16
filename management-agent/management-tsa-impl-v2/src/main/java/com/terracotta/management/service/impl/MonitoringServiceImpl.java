/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.service.MonitoringService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class MonitoringServiceImpl implements MonitoringService {

  private static final int MAX_DGC_STATS_ENTRIES = 1000;

  private final ServerManagementService serverManagementService;
  private final ClientManagementService clientManagementService;

  public MonitoringServiceImpl(ServerManagementService serverManagementService, ClientManagementService clientManagementService) {
    this.serverManagementService = serverManagementService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public Collection<StatisticsEntity> getClientStatistics(Set<String> clientIds, Set<String> attributes, Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClientsStatistics(clientIds, stringsToProductsIds(clientProductIds), attributes);
  }

  @Override
  public Collection<StatisticsEntity> getServerStatistics(Set<String> serverNames, Set<String> attributes) throws ServiceExecutionException {
    return serverManagementService.getServersStatistics(serverNames, attributes);
  }

  @Override
  public Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getDgcStatistics(serverNames, MAX_DGC_STATS_ENTRIES);
  }
}
