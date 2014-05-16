/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ConfigEntity;
import com.terracotta.management.service.ConfigurationService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class ConfigurationServiceImpl implements ConfigurationService {

  private final ClientManagementService clientManagementService;
  private final ServerManagementService serverManagementService;

  public ConfigurationServiceImpl(ServerManagementService serverManagementService, ClientManagementService clientManagementService) {
    this.clientManagementService = clientManagementService;
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<ConfigEntity> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getServerConfigs(serverNames);
  }

  @Override
  public Collection<ConfigEntity> getClientConfigs(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClientConfigs(clientIds, stringsToProductsIds(clientProductIds));
  }
}
