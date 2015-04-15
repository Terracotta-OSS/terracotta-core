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

import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.service.MonitoringService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class MonitoringServiceImpl implements MonitoringService {

  private static final int MAX_DGC_STATS_ENTRIES = 1000;

  private final ServerManagementService serverManagementService;
  private final ClientManagementService clientManagementService;

  public MonitoringServiceImpl(ServerManagementService serverManagementService, ClientManagementService clientManagementService) {
    this.serverManagementService = serverManagementService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public Collection<StatisticsEntity> getClientStatistics(Set<String> clientIds, Set<String> attributes, Set<String> clientProductIds) throws ServiceExecutionException {
    return clientManagementService.getClientsStatistics(clientIds, stringsToProductsIds(clientProductIds), attributes);
  }

  @Override
  public Collection<StatisticsEntity> getServerStatistics(Set<String> serverNames, Set<String> attributes) throws ServiceExecutionException {
    return serverManagementService.getServersStatistics(serverNames, attributes);
  }

  @Override
  public Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getDgcStatistics(serverNames, MAX_DGC_STATS_ENTRIES);
  }
}
