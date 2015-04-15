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

import com.terracotta.management.resource.ConfigEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA config facilities.
 *
 * @author Ludovic Orban
 */
public interface ConfigurationService {

  /**
   * Get a collection {@link com.terracotta.management.resource.ConfigEntity} objects each representing a server
   * config. Only requested servers are included, or all of them if serverNames is null.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<ConfigEntity> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get a collection {@link com.terracotta.management.resource.ConfigEntity} objects each representing a client
   * config. Only requested clients are included, or all of them if clientIds is null.
   *
   * @param clientIds A set of client IDs, null meaning all of them.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<ConfigEntity> getClientConfigs(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException;

}
