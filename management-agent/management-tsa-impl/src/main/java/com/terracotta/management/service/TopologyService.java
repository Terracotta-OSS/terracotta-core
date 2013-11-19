/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.tc.license.ProductID;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ServerGroupEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA topology querying facilities.

 * @author Ludovic Orban
 */
public interface TopologyService {

  /**
   * Get the topology of the current TSA
   * @return a collection of server groups
   * @throws ServiceExecutionException
   */
  Collection<ServerGroupEntity> getTopology() throws ServiceExecutionException;

  /**
   * Get the connected clients of the current TSA
   * @return a collection of currently connected clients
   * @throws ServiceExecutionException
   */
  Collection<ClientEntity> getClients(Set<ProductID> clientProductIds) throws ServiceExecutionException;

}
