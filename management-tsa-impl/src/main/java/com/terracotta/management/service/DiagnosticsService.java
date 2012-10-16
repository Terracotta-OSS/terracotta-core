/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ThreadDumpEntity;

import java.util.Collection;

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
  Collection<ThreadDumpEntity> getClusterThreadDump() throws ServiceExecutionException;

  /**
   * Run the Distributed Garbage Collector in the server array.
   *
   * @return true if the DGC cycle started, false otherwise.
   * @throws ServiceExecutionException
   */
  boolean runDgc() throws ServiceExecutionException;

}
