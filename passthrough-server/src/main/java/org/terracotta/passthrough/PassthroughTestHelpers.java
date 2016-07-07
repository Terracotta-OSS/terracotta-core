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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.Arrays;

/**
 * Used for setting up PassthroughClusterControl instances to wrap test cluster configurations, based on the passthrough classes.
 * It describes a single stripe, consisting of a single active and an optional passive.  Services for providing server-side
 * functionality, server-side entities, or client-side entities, are registered via the given ServerInitializer callback,
 * allowing common initialization code to be invoked on each server in the stripe.
 * It can be used by arbitrary testing systems as it acts as a library, not a framework.
 * 
 * Note that PassthroughClusterControl is returned instead of IClusterControl so that the caller can take responsibility for the
 * type-specific clean-up routines.
 */
public class PassthroughTestHelpers {
  /**
   * Creates a cluster consisting only of a single server in active state.
   * 
   * @param stripeName The unique name for this stripe.
   * @param initializer The callback to handle initialization of the server.
   * @return A control object to use for interacting with the cluster.
   */
  public static PassthroughClusterControl createActiveOnly(String stripeName, ServerInitializer initializer) {
    return createMultiServerStripe(stripeName, 1, initializer);
  }

  /**
   * Creates a cluster consisting only of 2 servers configured as 1 stripe:  an active and a passive.
   * 
   * @param stripeName The unique name for this stripe.
   * @param initializer The callback to handle initialization of both servers, called on each.
   * @return A control object to use for interacting with the cluster.
   */
  public static PassthroughClusterControl createActivePassive(String stripeName, ServerInitializer initializer) {
    return createMultiServerStripe(stripeName, 2, initializer);
  }

  /**
   * Creates a cluster consisting of any number of server, configured as a single stripe.
   * 
   * @param stripeName The unique name for this stripe.
   * @param numOfServers The number of server to create in the stripe.
   * @param initializer The callback to handle initialization of both servers, called on each.
   * @return A control object to use for interacting with the cluster.
   */
  public static PassthroughClusterControl createMultiServerStripe(String stripeName, int numOfServers, ServerInitializer initializer) {
    PassthroughServer[] servers = new PassthroughServer[numOfServers];
    for(int i = 0; i < numOfServers; i++) {
      servers[i] = intializeServer(initializer);
    }
    if(numOfServers == 1) {
      return new PassthroughClusterControl(stripeName, servers[0]);
    } else {
      return new PassthroughClusterControl(stripeName, servers[0], Arrays.copyOfRange(servers, 1, servers.length));
    }
  }

  private static PassthroughServer intializeServer(ServerInitializer initializer) {
    PassthroughServer activeServer = new PassthroughServer();
    initializer.registerServicesForServer(activeServer);
    return activeServer;
  }


  /**
   * In order to register any service providers, client-side and server-side entity provider services, an implementation of
   * this interface is used.
   * These cannot be set directly on the harness as each server in the stripe is expected to have its own provider instances.
   * This interface acts as a callback for when one of the servers on the stripe is about to be made active, so that it can
   * have those providers registered.  This also means that the implementation should be state-less, acting only on the
   * server instance it is given.
   */
  public interface ServerInitializer {
    public void registerServicesForServer(PassthroughServer server);
  }
}
