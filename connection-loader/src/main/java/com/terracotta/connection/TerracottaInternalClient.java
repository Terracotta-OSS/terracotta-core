/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import com.tc.object.ClientEntityManager;
import com.tc.object.locks.ClientLockManager;


public interface TerracottaInternalClient {

  /**
   * Initialize the client. This will start the client to connect to the L2.
   */
  void init();

  /**
   * Shuts down the client
   */
  void shutdown();

  /**
   * Returns whether this client has been shutdown or not
   */
  boolean isShutdown();

  /**
   * Returns whether this client is initialized or not
   */
  boolean isInitialized();

  String getUuid();
  
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
