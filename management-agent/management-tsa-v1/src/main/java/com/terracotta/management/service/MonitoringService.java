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
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.StatisticsEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA statistics monitoring facilities.
 *
 * @author Ludovic Orban
 */
public interface MonitoringService {

  /**
   * Get the statistics of the specified client.
   *
   * @param clientIds A set of client IDs, null meaning all of them.
   * @param attributes the attributes to include in the response, all know ones will be returned if null.
   * @return a collection of {@link StatisticsEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<StatisticsEntity> getClientStatistics(Set<String> clientIds, Set<String> attributes, Set<String> clientProductIds) throws ServiceExecutionException;

  /**
   * Get the statistics of the specified server.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @param attributes the attributes to include in the response, all know ones will be returned if null.
   * @return a collection of {@link StatisticsEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<StatisticsEntity> getServerStatistics(Set<String> serverNames, Set<String> attributes) throws ServiceExecutionException;

  /**
   * Get the DGC statistics.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a {@link Collection} object of {@link StatisticsEntity} objects representing the DGC statistics,
   * one {@link StatisticsEntity} per DGC iteration.
   * @throws ServiceExecutionException
   */
  Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames) throws ServiceExecutionException;

}
