/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;
import com.terracotta.management.service.TopologyServiceV2;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class TopologyServiceImplV2 implements TopologyServiceV2 {

  private final ServerManagementServiceV2 serverManagementService;
  private final ClientManagementServiceV2 clientManagementService;

  public TopologyServiceImplV2(ServerManagementServiceV2 serverManagementService, ClientManagementServiceV2 clientManagementService) {
    this.serverManagementService = serverManagementService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public Collection<ServerGroupEntityV2> getServerGroups(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getServerGroups(serverNames);
  }

  @Override
  public Collection<ClientEntityV2> getClients(Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClients(null, stringsToProductsIds(clientProductIds));
  }

}
