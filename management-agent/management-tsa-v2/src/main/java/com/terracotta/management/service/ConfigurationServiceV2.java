/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.ConfigEntityV2;

import java.util.Set;

/**
 * An interface for service implementations providing TSA config facilities.
 *
 * @author Ludovic Orban
 */
public interface ConfigurationServiceV2 {

  /**
   * Get a collection {@link com.terracotta.management.resource.ConfigEntityV2} objects each representing a server
   * config. Only requested servers are included, or all of them if serverNames is null.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<ConfigEntityV2> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get a collection {@link com.terracotta.management.resource.ConfigEntityV2} objects each representing a client
   * config. Only requested clients are included, or all of them if clientIds is null.
   *
   * @param clientIds A set of client IDs, null meaning all of them.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<ConfigEntityV2> getClientConfigs(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException;

}
