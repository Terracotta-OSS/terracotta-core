/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
