/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ConfigEntityV2;
import com.terracotta.management.service.ConfigurationServiceV2;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class ConfigurationServiceImplV2 implements ConfigurationServiceV2 {

  private final ClientManagementServiceV2 clientManagementService;
  private final ServerManagementServiceV2 serverManagementService;

  public ConfigurationServiceImplV2(ServerManagementServiceV2 serverManagementService, ClientManagementServiceV2 clientManagementService) {
    this.clientManagementService = clientManagementService;
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<ConfigEntityV2> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getServerConfigs(serverNames);
  }

  @Override
  public Collection<ConfigEntityV2> getClientConfigs(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClientConfigs(clientIds, stringsToProductsIds(clientProductIds));
  }
}
