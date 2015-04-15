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

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.ThreadDumpEntityV2;
import com.terracotta.management.resource.TopologyReloadStatusEntityV2;
import com.terracotta.management.service.DiagnosticsServiceV2;

import java.util.Set;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

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
  public ResponseEntityV2<ThreadDumpEntityV2> getClusterThreadDump(Set<String> clientProductIds) throws ServiceExecutionException {
    ResponseEntityV2<ThreadDumpEntityV2> responseEntityV2 = serverManagementService.serversThreadDump(null);
    responseEntityV2.getEntities().addAll(clientManagementService.clientsThreadDump(null, stringsToProductsIds(clientProductIds)).getEntities());
    return responseEntityV2;
  }

  @Override
  public ResponseEntityV2<ThreadDumpEntityV2> getServersThreadDump(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.serversThreadDump(serverNames);
  }

  @Override
  public ResponseEntityV2<ThreadDumpEntityV2> getClientsThreadDump(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException {
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
  public ResponseEntityV2<TopologyReloadStatusEntityV2> reloadConfiguration(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.reloadConfiguration(serverNames);
  }

}
