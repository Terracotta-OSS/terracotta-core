/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA diagnostics facilities.
 *
 * @author Ludovic Orban
 */
public interface DiagnosticsService {

  /**
   * Get a collection {@link ThreadDumpEntity} objects each representing a server or client
   * thread dump. All connected servers and clients are included.
   *
   * @return a collection {@link ThreadDumpEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<ThreadDumpEntity> getClusterThreadDump(Set<String> clientProductIds) throws ServiceExecutionException;

  /**
   * Get a collection {@link ThreadDumpEntity} objects each representing a server
   * thread dump. Only requested servers are included, or all of them if serverNames is null.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection {@link ThreadDumpEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<ThreadDumpEntity> getServersThreadDump(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get a collection {@link ThreadDumpEntity} objects each representing a client
   * thread dump. Only requested clients are included, or all of them if clientIds is null.
   *
   * @param clientIds A set of client IDs, null meaning all of them.
   * @return a collection {@link ThreadDumpEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<ThreadDumpEntity> getClientsThreadDump(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException;

  /**
   * Run the Distributed Garbage Collector in the server array.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return true if the DGC cycle started, false otherwise.
   * @throws ServiceExecutionException
   */
  boolean runDgc(Set<String> serverNames) throws ServiceExecutionException;

  boolean dumpClusterState(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Reload TSA configuration.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection {@link TopologyReloadStatusEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<TopologyReloadStatusEntity> reloadConfiguration(Set<String> serverNames) throws ServiceExecutionException;

}
