/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;
import com.terracotta.management.resource.TopologyEntityV2;
import com.terracotta.management.service.OperatorEventsServiceV2;
import com.terracotta.management.service.TopologyServiceV2;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

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

  private Collection<ClientEntityV2> getClients(Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClients(null, stringsToProductsIds(clientProductIds));
  }

  @Override
  public Collection<TopologyEntityV2> getTopologies(Set<String> productIDs) throws ServiceExecutionException {
     TopologyEntityV2 result = new TopologyEntityV2();
     result.setVersion(this.getClass().getPackage().getImplementationVersion());
     result.getServerGroupEntities().addAll(this.getServerGroups(null));
     result.getClientEntities().addAll(this.getClients(productIDs));
     result.setUnreadOperatorEventCount(operatorEventsService.getUnreadCount(null));
     return Collections.singleton(result);
  }

  @Override
  public Collection<TopologyEntityV2> getServerTopologies(Set<String> serverNames) throws ServiceExecutionException {
     TopologyEntityV2 result = new TopologyEntityV2();
     result.setVersion(this.getClass().getPackage().getImplementationVersion());
     result.getServerGroupEntities().addAll(this.getServerGroups(serverNames));
     result.setUnreadOperatorEventCount(operatorEventsService.getUnreadCount(serverNames));
     return Collections.singleton(result);
  }

  @Override
  public Collection<TopologyEntityV2> getConnectedClients(Set<String> productIDs) throws ServiceExecutionException {
    TopologyEntityV2 result = new TopologyEntityV2();
    result.setVersion(this.getClass().getPackage().getImplementationVersion());
    result.getClientEntities().addAll(this.getClients(productIDs));
    return Collections.singleton(result);
  }

  @Override
  public Collection<TopologyEntityV2> getUnreadOperatorEventCount(Set<String> serverNames) throws ServiceExecutionException {
    TopologyEntityV2 result = new TopologyEntityV2();
    result.setVersion(this.getClass().getPackage().getImplementationVersion());
    result.setUnreadOperatorEventCount(operatorEventsService.getUnreadCount(serverNames));
    return Collections.singleton(result);
  }

  @Override
  public String getLocalServerName() throws ServiceExecutionException {
    return serverManagementService.getLocalServerName();
  }

}
