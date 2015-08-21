/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import java.util.Set;

import com.tc.object.ClientEntityManager;
import com.tc.object.locks.ClientLockManager;


public interface ClientHandle {

  void activateTunnelledMBeanDomains(Set<String> tunnelledMBeanDomains);

  void shutdown();

  /**
   * Required for TerracottaConnectionService Connection instantiation.
   * @return The client entity manager for end-points managed by this client.
   */
  ClientEntityManager getClientEntityManager();
  
  /**
   * Required for TerracottaConnectionService Connection instantiation.
   * @return The client lock manager for end-points managed by this client.
   */
  ClientLockManager getClientLockManager();
}
