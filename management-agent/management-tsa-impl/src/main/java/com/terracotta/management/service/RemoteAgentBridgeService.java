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
   * Get the connected remote agent node names and details. This goes over the network to fetch details.
   *
   * @return a map using the remote agent node names as keys and a map of attributes as values.
   * @throws ServiceExecutionException
   */
  Map<String, Map<String, String>> getRemoteAgentNodeDetails() throws ServiceExecutionException;

  /**
   * Invoke an method on the remote agent.
   *
   * @param nodeName the remote agent node name.
   * @param remoteCallDescriptor the remote call descriptor.
   * @return the serialized response.
   * @throws ServiceExecutionException
   */
  byte[] invokeRemoteMethod(String nodeName, RemoteCallDescriptor remoteCallDescriptor) throws ServiceExecutionException;

  /**
   * Get the remote agent call timeout previously set. If none was set, the default one is returned. The bridge will wait for at most the time
   * returned by this method for bridged calls to return.
   *
   * @return the remote agent call timeout.
   */
  long getCallTimeout();

  /**
   * Set the remote agent call timeout for the current thread.
   *
   * @param timeout the remote agent call timeout.
   */
  void setCallTimeout(long timeout);

  /**
   * Clear the remote agent call timeout for the current thread.
   */
  void clearCallTimeout();

}
