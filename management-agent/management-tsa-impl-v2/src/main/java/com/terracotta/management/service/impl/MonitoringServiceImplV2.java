/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.StatisticsEntityV2;
import com.terracotta.management.service.MonitoringServiceV2;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class MonitoringServiceImplV2 implements MonitoringServiceV2 {

  private static final int MAX_DGC_STATS_ENTRIES = 1000;

  private final ServerManagementServiceV2 serverManagementService;
  private final ClientManagementServiceV2 clientManagementService;

  public MonitoringServiceImplV2(ServerManagementServiceV2 serverManagementService, ClientManagementServiceV2 clientManagementService) {
    this.serverManagementService = serverManagementService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public Collection<StatisticsEntityV2> getClientStatistics(Set<String> clientIds, Set<String> attributes, Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClientsStatistics(clientIds, stringsToProductsIds(clientProductIds), attributes);
  }

  @Override
  public Collection<StatisticsEntityV2> getServerStatistics(Set<String> serverNames, Set<String> attributes) throws ServiceExecutionException {
    return serverManagementService.getServersStatistics(serverNames, attributes);
  }

  @Override
  public Collection<StatisticsEntityV2> getDgcStatistics(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getDgcStatistics(serverNames, MAX_DGC_STATS_ENTRIES);
  }
}
