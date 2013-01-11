/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;
import com.terracotta.management.service.DiagnosticsService;
import com.terracotta.management.service.TsaManagementClientService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class DiagnosticsServiceImpl implements DiagnosticsService {

  private final TsaManagementClientService tsaManagementClientService;

  public DiagnosticsServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public Collection<ThreadDumpEntity> getClusterThreadDump() throws ServiceExecutionException {
    Collection<ThreadDumpEntity> threadDumpEntities = new ArrayList<ThreadDumpEntity>();

    threadDumpEntities.addAll(tsaManagementClientService.serversThreadDump(null));
    threadDumpEntities.addAll(tsaManagementClientService.clientsThreadDump(null));

    return threadDumpEntities;
  }

  @Override
  public Collection<ThreadDumpEntity> getServersThreadDump(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.serversThreadDump(serverNames);
  }

  @Override
  public Collection<ThreadDumpEntity> getClientsThreadDump(Set<String> clientIds) throws ServiceExecutionException {
    return tsaManagementClientService.clientsThreadDump(clientIds);
  }

  @Override
  public boolean runDgc() throws ServiceExecutionException {
    return tsaManagementClientService.runDgc();
  }

  @Override
  public Collection<TopologyReloadStatusEntity> reloadConfiguration() throws ServiceExecutionException {
    return tsaManagementClientService.reloadConfiguration();
  }

}
