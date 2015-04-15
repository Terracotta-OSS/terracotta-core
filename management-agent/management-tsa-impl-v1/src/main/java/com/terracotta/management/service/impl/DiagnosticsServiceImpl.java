/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
