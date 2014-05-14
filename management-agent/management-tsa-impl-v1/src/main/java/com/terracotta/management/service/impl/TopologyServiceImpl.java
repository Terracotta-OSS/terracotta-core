/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ServerGroupEntity;
import com.terracotta.management.service.TopologyService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class TopologyServiceImpl implements TopologyService {

  private final ServerManagementService serverManagementService;
  private final ClientManagementService clientManagementService;

  public TopologyServiceImpl(ServerManagementService serverManagementService, ClientManagementService clientManagementService) {
    this.serverManagementService = serverManagementService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public Collection<ServerGroupEntity> getServerGroups(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getServerGroups(serverNames);
  }

  @Override
  public Collection<ClientEntity> getClients(Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClients(null, stringsToProductsIds(clientProductIds));
  }

}
