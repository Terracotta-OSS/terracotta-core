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

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;
import com.terracotta.management.resource.TopologyEntityV2;
import com.terracotta.management.service.OperatorEventsServiceV2;
import com.terracotta.management.service.TopologyServiceV2;

import java.util.Collection;
import java.util.Set;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

/**
 * @author Ludovic Orban
 */
public class TopologyServiceImplV2 implements TopologyServiceV2 {

  private final ServerManagementServiceV2 serverManagementService;
  private final ClientManagementServiceV2 clientManagementService;
  private final OperatorEventsServiceV2 operatorEventsService;

  public TopologyServiceImplV2(ServerManagementServiceV2 serverManagementService, ClientManagementServiceV2 clientManagementService, OperatorEventsServiceV2 operatorEventsService) {
    this.serverManagementService = serverManagementService;
    this.clientManagementService = clientManagementService;
    this.operatorEventsService = operatorEventsService;
  }

  private Collection<ServerGroupEntityV2> getServerGroups(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getServerGroups(serverNames);
  }

  private ResponseEntityV2<ClientEntityV2> getClients(Set<String> clientProductIds, Set<String> clientIds) throws ServiceExecutionException {
    return clientManagementService.getClients(clientIds, stringsToProductsIds(clientProductIds));
  }

  @Override
  public ResponseEntityV2<TopologyEntityV2> getTopologies(Set<String> productIDs) throws ServiceExecutionException {
    ResponseEntityV2<TopologyEntityV2> response = new ResponseEntityV2<TopologyEntityV2>();
    TopologyEntityV2 topology = new TopologyEntityV2();
    topology.getServerGroupEntities().addAll(this.getServerGroups(null));
    ResponseEntityV2<ClientEntityV2> clientsResponse = this.getClients(productIDs, null);
    topology.getClientEntities().addAll(clientsResponse.getEntities());
    response.getExceptionEntities().addAll(clientsResponse.getExceptionEntities());
    topology.getUnreadOperatorEventCount().putAll(operatorEventsService.getUnreadCount(null));
    response.getEntities().add(topology);
    return response;
  }

  @Override
  public ResponseEntityV2<TopologyEntityV2> getServerTopologies(Set<String> serverNames) throws ServiceExecutionException {
    ResponseEntityV2<TopologyEntityV2> response = new ResponseEntityV2<TopologyEntityV2>();
    TopologyEntityV2 topology = new TopologyEntityV2();
    topology.getServerGroupEntities().addAll(this.getServerGroups(serverNames));
    topology.getUnreadOperatorEventCount().putAll(operatorEventsService.getUnreadCount(serverNames));
    response.getEntities().add(topology);
    return response;
  }

  @Override
  public ResponseEntityV2<TopologyEntityV2> getConnectedClients(Set<String> productIDs, Set<String> clientIDs) throws ServiceExecutionException {
    ResponseEntityV2<TopologyEntityV2> response = new ResponseEntityV2<TopologyEntityV2>();
    TopologyEntityV2 topology = new TopologyEntityV2();
    ResponseEntityV2<ClientEntityV2> clientsResponse = this.getClients(productIDs, clientIDs);
    topology.getClientEntities().addAll(clientsResponse.getEntities());
    response.getExceptionEntities().addAll(clientsResponse.getExceptionEntities());
    response.getEntities().add(topology);
    return response;
  }

  @Override
  public ResponseEntityV2<TopologyEntityV2> getUnreadOperatorEventCount(Set<String> serverNames) throws ServiceExecutionException {
    ResponseEntityV2<TopologyEntityV2> response = new ResponseEntityV2<TopologyEntityV2>();
    TopologyEntityV2 topology = new TopologyEntityV2();
    topology.getUnreadOperatorEventCount().putAll(operatorEventsService.getUnreadCount(serverNames));
    response.getEntities().add(topology);
    return response;
  }

}
