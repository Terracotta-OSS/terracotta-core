/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.tc.license.ProductID;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;
import com.terracotta.management.service.DiagnosticsService;
import com.terracotta.management.service.TsaManagementClientService;

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
  public Collection<ThreadDumpEntity> getClusterThreadDump(Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return tsaManagementClientService.clusterThreadDump(clientProductIds);
  }

  @Override
  public Collection<ThreadDumpEntity> getServersThreadDump(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.serversThreadDump(serverNames);
  }

  @Override
  public Collection<ThreadDumpEntity> getClientsThreadDump(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return tsaManagementClientService.clientsThreadDump(clientIds, clientProductIds);
  }

  @Override
  public boolean runDgc(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.runDgc(serverNames);
  }

  @Override
  public boolean dumpClusterState(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.dumpClusterState(serverNames);
  }

  @Override
  public Collection<TopologyReloadStatusEntity> reloadConfiguration() throws ServiceExecutionException {
    return tsaManagementClientService.reloadConfiguration();
  }

}
