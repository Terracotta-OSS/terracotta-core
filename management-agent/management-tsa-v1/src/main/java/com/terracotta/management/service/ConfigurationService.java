/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
