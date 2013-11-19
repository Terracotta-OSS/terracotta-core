/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.tc.license.ProductID;
import com.terracotta.management.resource.ConfigEntity;
import com.terracotta.management.service.ConfigurationService;
import com.terracotta.management.service.TsaManagementClientService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class ConfigurationServiceImpl implements ConfigurationService {

  private final TsaManagementClientService tsaManagementClientService;

  public ConfigurationServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public Collection<ConfigEntity> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.getServerConfigs(serverNames);
  }

  @Override
  public Collection<ConfigEntity> getClientConfigs(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return tsaManagementClientService.getClientConfigs(clientIds, clientProductIds);
  }
}
