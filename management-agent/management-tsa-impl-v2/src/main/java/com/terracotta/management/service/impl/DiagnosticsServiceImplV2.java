/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;
import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ThreadDumpEntityV2;
import com.terracotta.management.resource.TopologyReloadStatusEntityV2;
import com.terracotta.management.service.DiagnosticsServiceV2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class DiagnosticsServiceImplV2 implements DiagnosticsServiceV2 {

  private final ServerManagementServiceV2 serverManagementService;
  private final ClientManagementServiceV2 clientManagementService;

  public DiagnosticsServiceImplV2(ServerManagementServiceV2 serverManagementService, ClientManagementServiceV2 clientManagementService) {
    this.serverManagementService = serverManagementService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public Collection<ThreadDumpEntityV2> getClusterThreadDump(Set<String> clientProductIds) throws ServiceExecutionException {
    Collection<ThreadDumpEntityV2> result = new ArrayList<ThreadDumpEntityV2>();
    result.addAll(serverManagementService.serversThreadDump(null));
    result.addAll(clientManagementService.clientsThreadDump(null, stringsToProductsIds(clientProductIds)));
    return result;
  }

  @Override
  public Collection<ThreadDumpEntityV2> getServersThreadDump(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.serversThreadDump(serverNames);
  }

  @Override
  public Collection<ThreadDumpEntityV2> getClientsThreadDump(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException {
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
  public Collection<TopologyReloadStatusEntityV2> reloadConfiguration(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.reloadConfiguration(serverNames);
  }

}
