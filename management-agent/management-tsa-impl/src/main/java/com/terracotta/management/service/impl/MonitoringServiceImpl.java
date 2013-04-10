/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.service.MonitoringService;
import com.terracotta.management.service.TsaManagementClientService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class MonitoringServiceImpl implements MonitoringService {

  private static final int MAX_DGC_STATS_ENTRIES = 1000;

  private final TsaManagementClientService tsaManagementClientService;

  public MonitoringServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public Collection<StatisticsEntity> getClientStatistics(Set<String> clientIds, Set<String> attributes) throws ServiceExecutionException {
    return tsaManagementClientService.getClientsStatistics(clientIds, attributes);
  }

  @Override
  public Collection<StatisticsEntity> getServerStatistics(Set<String> serverNames, Set<String> attributes) throws ServiceExecutionException {
    return tsaManagementClientService.getServersStatistics(serverNames, attributes);
  }

  @Override
  public Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.getDgcStatistics(serverNames, MAX_DGC_STATS_ENTRIES);
  }
}
