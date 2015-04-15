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

import com.terracotta.management.resource.TopologyEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA topology querying facilities.

 * @author Ludovic Orban
 */
public interface TopologyService {

  /**
   * Get the server topology of the current TSA
   * 
   * @return a collection of server names
   * @throws ServiceExecutionException
   */
  Collection<TopologyEntity> getServerTopologies(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get the unread operator events of the current TSA
   * 
   * @return a collection of server names
   * @throws ServiceExecutionException
   */
  Collection<TopologyEntity> getUnreadOperatorEventCount(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get the connected clients of the current TSA
   * 
   * @return a collection of productIds
   * @throws ServiceExecutionException
   */
  Collection<TopologyEntity> getConnectedClients(Set<String> productIDs) throws ServiceExecutionException;

  /**
   * Get the topology of the current TSA
   * 
   * @return a collection of productIds
   * @throws ServiceExecutionException
   */
  Collection<TopologyEntity> getTopologies(Set<String> productIDs) throws ServiceExecutionException;

}
