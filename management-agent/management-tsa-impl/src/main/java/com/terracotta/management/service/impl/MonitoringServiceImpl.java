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
  public Set<String> getAllClientIds() throws ServiceExecutionException {
    return tsaManagementClientService.getAllClientIds();
  }

  @Override
  public Set<String> getAllServerNames() throws ServiceExecutionException {
    return tsaManagementClientService.getAllServerNames();
  }

  @Override
  public StatisticsEntity getClientStatistics(String clientId, Set<String> attributes) throws ServiceExecutionException {
    return tsaManagementClientService.getClientStatistics(clientId, attributes);
  }

  @Override
  public StatisticsEntity getServerStatistics(String serverName, Set<String> attributes) throws ServiceExecutionException {
    return tsaManagementClientService.getServerStatistics(serverName, attributes);
  }

  @Override
  public Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.getDgcStatistics(serverNames, MAX_DGC_STATS_ENTRIES);
  }
}
