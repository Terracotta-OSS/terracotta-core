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
