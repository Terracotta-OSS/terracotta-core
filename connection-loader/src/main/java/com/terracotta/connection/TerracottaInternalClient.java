/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.terracotta.connection;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.object.ClientEntityManager;
import com.tc.object.locks.ClientLockManager;
import java.util.concurrent.TimeoutException;


public interface TerracottaInternalClient {

  /**
   * Initialize the client. This will start the client to connect to the L2.
   */
  void init() throws TimeoutException, InterruptedException, ConfigurationSetupException;

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
