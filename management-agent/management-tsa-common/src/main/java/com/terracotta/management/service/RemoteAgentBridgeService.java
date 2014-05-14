/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;

import java.util.Map;
import java.util.Set;

/**
 * An interface for service implementations providing an abstraction of remote agents bridging.
 *
 * @author Ludovic Orban
 */
public interface RemoteAgentBridgeService {

  /**
   * Get the connected remote agents node names. This call does not go over the network.
   *
   * @return a set of connected remote agents node names.
   * @throws ServiceExecutionException
   */
  Set<String> getRemoteAgentNodeNames() throws ServiceExecutionException;

  /**
   * Get the connected remote agent node details. This goes over the network to fetch details.
   *
   * @param remoteAgentName the remote agent node name.
   * @return a map of attributes as values.
   * @throws ServiceExecutionException
   */
  Map<String, String> getRemoteAgentNodeDetails(String remoteAgentName) throws ServiceExecutionException;

  /**
   * Invoke an method on the remote agent.
   *
   * @param nodeName the remote agent node name.
   * @param remoteCallDescriptor the remote call descriptor.
   * @return the serialized response.
   * @throws ServiceExecutionException
   */
  byte[] invokeRemoteMethod(String nodeName, RemoteCallDescriptor remoteCallDescriptor) throws ServiceExecutionException;

}
