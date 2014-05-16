/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA topology querying facilities.

 * @author Ludovic Orban
 */
public interface TopologyServiceV2 {

  /**
   * Get the topology of the current TSA
   * @return a collection of server groups
   * @throws ServiceExecutionException
   */
  Collection<ServerGroupEntityV2> getServerGroups(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get the connected clients of the current TSA
   * @return a collection of currently connected clients
   * @throws ServiceExecutionException
   */
  Collection<ClientEntityV2> getClients(Set<String> clientProductIds) throws ServiceExecutionException;

}
