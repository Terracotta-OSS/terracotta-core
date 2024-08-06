/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.terracotta.connection;

import com.tc.object.ClientEntityManager;
import com.terracotta.connection.api.DetailedConnectionException;


public interface TerracottaInternalClient {

  /**
   * Initialize the client. This will start the client to connect to the L2.
   */
  void init() throws DetailedConnectionException;

  /**
   * Shuts down the client
   */
  void shutdown();

  /**
   * Returns whether this client has been shutdown or not
   */
  boolean isShutdown();
  
  /**
   * Required for TerracottaConnectionService Connection instantiation.
   * @return The client entity manager for end-points managed by this client.
   */
  ClientEntityManager getClientEntityManager();
}
