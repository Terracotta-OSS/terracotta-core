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

import com.terracotta.management.resource.ConfigEntityV2;
import com.terracotta.management.service.ConfigurationServiceV2;

import java.util.Set;

import static com.terracotta.management.resource.services.utils.ProductIdConverter.stringsToProductsIds;

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
  public ResponseEntityV2<ConfigEntityV2> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getServerConfigs(serverNames);
  }

  @Override
  public ResponseEntityV2<ConfigEntityV2> getClientConfigs(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClientConfigs(clientIds, stringsToProductsIds(clientProductIds));
  }
}
