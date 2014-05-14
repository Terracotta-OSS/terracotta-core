/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;
import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;
import com.terracotta.management.service.DiagnosticsService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class DiagnosticsServiceImpl implements DiagnosticsService {

  private final ServerManagementService serverManagementService;
  private final ClientManagementService clientManagementService;

  public DiagnosticsServiceImpl(ServerManagementService serverManagementService, ClientManagementService clientManagementService) {
    this.serverManagementService = serverManagementService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public Collection<ThreadDumpEntity> getClusterThreadDump(Set<String> clientProductIds) throws ServiceExecutionException {
    Collection<ThreadDumpEntity> result = new ArrayList<ThreadDumpEntity>();
    result.addAll(serverManagementService.serversThreadDump(null));
    result.addAll(clientManagementService.clientsThreadDump(null, stringsToProductsIds(clientProductIds)));
    return result;
  }

  @Override
  public Collection<ThreadDumpEntity> getServersThreadDump(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.serversThreadDump(serverNames);
  }

  @Override
  public Collection<ThreadDumpEntity> getClientsThreadDump(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.clientsThreadDump(clientIds, stringsToProductsIds(clientProductIds));
  }

  @Override
  public boolean runDgc(Set<String> serverNames) throws ServiceExecutionException {
    serverManagementService.runDgc(serverNames);
    return true;
  }

  @Override
  public boolean dumpClusterState(Set<String> serverNames) throws ServiceExecutionException {
    serverManagementService.dumpClusterState(serverNames);
    return true;
  }

  @Override
  public Collection<TopologyReloadStatusEntity> reloadConfiguration(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.reloadConfiguration(serverNames);
  }

}
