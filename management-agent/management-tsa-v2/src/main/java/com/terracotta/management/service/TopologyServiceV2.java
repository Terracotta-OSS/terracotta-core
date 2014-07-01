/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.TopologyEntityV2;

import java.util.Set;

/**
 * An interface for service implementations providing TSA topology querying facilities.

 * @author Ludovic Orban
 */
public interface TopologyServiceV2 {

  /**
   * Get the server topology of the current TSA
   * 
   * @return a collection of server names
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<TopologyEntityV2> getServerTopologies(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get the unread operator events of the current TSA
   * 
   * @return a collection of server names
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<TopologyEntityV2> getUnreadOperatorEventCount(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get the connected clients of the current TSA
   * 
   * @return a collection of productIds
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<TopologyEntityV2> getConnectedClients(Set<String> productIDs, Set<String> clientIDs) throws ServiceExecutionException;

  /**
   * Get the topology of the current TSA
   * 
   * @return a collection of productIds
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<TopologyEntityV2> getTopologies(Set<String> productIDs) throws ServiceExecutionException;

}
