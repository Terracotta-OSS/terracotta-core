/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.Map;


/**
 * Exists to support PassthroughConnectionService.  No context can be passed into that service so this represents the shared
 * static interaction point, between PassthroughConnectionService and the testing code.
 * The idea is that each server is registered with a name and that makes the server addressable by URI:
 * -"serverName" is addressed as "passthrough://serverName"
 */
public class PassthroughServerRegistry {
  private static PassthroughServerRegistry sharedInstance;

  /**
   * Lazily creates the shared instance, if it doesn't already exist.
   * 
   * @return The shared registry instance
   */
  public static PassthroughServerRegistry getSharedInstance() {
    if (null == PassthroughServerRegistry.sharedInstance) {
      PassthroughServerRegistry.sharedInstance = new PassthroughServerRegistry();
    }
    return PassthroughServerRegistry.sharedInstance;
  }

  private final Map<String, PassthroughServer> servers;

  private PassthroughServerRegistry() {
    this.servers = new HashMap<String, PassthroughServer>();
  }

  /**
   * Registers a passthrough server with the given serverName.
   * 
   * @param serverName The name to use in addressing this server
   * @param server The server to register
   * @return The server previously registered with this name
   */
  public PassthroughServer registerServer(String serverName, PassthroughServer server) {
    return this.servers.put(serverName, server);
  }

  /**
   * Explicitly unregisters the server which had been registered with the given serverName.
   * 
   * @param serverName The name to unregister
   * @return The instance which had been registered by this name
   */
  public PassthroughServer unregisterServer(String serverName) {
    return this.servers.remove(serverName);
  }

  /**
   * Gets the server registered as serverName.
   * 
   * @param serverName The name of the server to look-up
   * @return The server instance
   */
  public PassthroughServer getServerForName(String serverName) {
    return this.servers.get(serverName);
  }
}
