/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.ThreadDumpEntityV2;
import com.terracotta.management.resource.TopologyReloadStatusEntityV2;

import java.util.Set;

/**
 * An interface for service implementations providing TSA diagnostics facilities.
 *
 * @author Ludovic Orban
 */
public interface DiagnosticsServiceV2 {

  /**
   * Get a collection {@link ThreadDumpEntityV2} objects each representing a server or client
   * thread dump. All connected servers and clients are included.
   *
   * @return a collection {@link ThreadDumpEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<ThreadDumpEntityV2> getClusterThreadDump(Set<String> clientProductIds) throws ServiceExecutionException;

  /**
   * Get a collection {@link ThreadDumpEntityV2} objects each representing a server
   * thread dump. Only requested servers are included, or all of them if serverNames is null.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection {@link ThreadDumpEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<ThreadDumpEntityV2> getServersThreadDump(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get a collection {@link ThreadDumpEntityV2} objects each representing a client
   * thread dump. Only requested clients are included, or all of them if clientIds is null.
   *
   * @param clientIds A set of client IDs, null meaning all of them.
   * @return a collection {@link ThreadDumpEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<ThreadDumpEntityV2> getClientsThreadDump(Set<String> clientIds, Set<String> clientProductIds) throws ServiceExecutionException;

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
   * @return a collection {@link TopologyReloadStatusEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<TopologyReloadStatusEntityV2> reloadConfiguration(Set<String> serverNames) throws ServiceExecutionException;

}
