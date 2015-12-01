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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.junit.Assert;
import org.terracotta.connection.Connection;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;


/**
 * The top-level "server" of the passthrough testing system.
 * The server can be put into either an "active" or "passive" mode when constructed.  This determines whether server can have
 * downstream passives attached to it.
 */
public class PassthroughServer {
  private PassthroughServerProcess serverProcess;
  private boolean hasStarted;
  private final List<EntityClientService<?, ?>> entityClientServices;
  // Note that we don't currently use the connection ID outside of this class but it is convenient and may be exposed outside, later.
  private long nextConnectionID;
  
  // We also track various information for the restart case.
  private final List<ServerEntityService<?, ?>> savedServerEntityServices;
  private final Map<Class<?>, ServiceProvider> savedServiceProviders;
  private final Map<Class<?>, ServiceProviderConfiguration> savedServiceProviderConfigurations;
  private final Map<Long, PassthroughConnection> savedClientConnections;
  private PassthroughServer savedPassiveServer;
  
  public PassthroughServer(boolean isActiveMode) {
    this.serverProcess = new PassthroughServerProcess(isActiveMode);
    this.entityClientServices = new Vector<EntityClientService<?, ?>>();
    this.nextConnectionID = 1;
    
    // Create the containers we will use for tracking the state we will need to repopulate on restart.
    this.savedServerEntityServices = new Vector<ServerEntityService<?, ?>>();
    this.savedServiceProviders = new HashMap<Class<?>, ServiceProvider>();
    this.savedServiceProviderConfigurations = new HashMap<Class<?>, ServiceProviderConfiguration>();
    this.savedClientConnections = new HashMap<Long, PassthroughConnection>();
    
    // Register built-in services.
    PassthroughCommunicatorServiceProvider communicatorServiceProvider = new PassthroughCommunicatorServiceProvider();
    registerServiceProvider(ClientCommunicator.class, communicatorServiceProvider, null);
  }

  public void registerServerEntityService(ServerEntityService<?, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.savedServerEntityServices.add(service);
    this.serverProcess.registerEntityService(service);
  }

  public void registerClientEntityService(EntityClientService<?, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.entityClientServices.add(service);
  }

  public synchronized Connection connectNewClient() {
    Assert.assertTrue(this.hasStarted);
    final long thisConnectionID = this.nextConnectionID;
    this.nextConnectionID += 1;
    // Note that we need to track the connections for reconnect so pass in this cleanup routine to remove it from our tracking.
    Runnable onClose = new Runnable() {
      @Override
      public void run() {
        synchronized (PassthroughServer.this) {
          PassthroughServer.this.savedClientConnections.remove(thisConnectionID);
        }
      }
    };
    PassthroughConnection connection = new PassthroughConnection(this.serverProcess, this.entityClientServices, onClose);
    this.savedClientConnections.put(thisConnectionID, connection);
    return connection;
  }

  public void start() {
    this.hasStarted = true;
    boolean shouldLoadStorage = false;
    this.serverProcess.start(shouldLoadStorage);
  }

  public void stop() {
    this.serverProcess.shutdown();
  }

  public <T> void registerServiceProviderForType(Class<T> clazz, ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    registerServiceProvider(clazz, serviceProvider, providerConfiguration);
  }

  public void attachDownstreamPassive(PassthroughServer passiveServer) {
    this.savedPassiveServer = passiveServer;
    this.serverProcess.setDownstreamPassiveServerProcess(passiveServer.serverProcess);
  }

  /**
   * Called to act as though the server suddenly crashed and then restarted.  The method returns only when the server is
   * back up, ready to receive reconnects (potentially having already handled them) and/or new calls.
   * 
   * NOTE:  This will always result in a fail-over if there is a downstream passive.
   * 
   * @return The new active server (typically this but could be a previous passive)
   */
  public PassthroughServer restart() {
    // Disconnect all connections before shutdown.
    for(PassthroughConnection connection : this.savedClientConnections.values()) {
      connection.disconnect();
    }
    // Shut down the server processes.  Note that this is just stopping the processes since we want to clear the message queues.  We will only actually restart the state of the active.
    // First, the active.
    this.serverProcess.shutdown();
    if (null != this.savedPassiveServer) {
      // Now, the passive.
      this.savedPassiveServer.serverProcess.shutdown();
    }
    // Start a new one.
    // (we will make it active if it is the only server in the stripe, otherwise it will become passive when we fail-over).
    boolean isActiveMode = (null == this.savedPassiveServer);
    this.serverProcess = new PassthroughServerProcess(isActiveMode);
    // Populate the server with its services.
    for (ServerEntityService<?, ?> serverEntityService : this.savedServerEntityServices) {
      this.serverProcess.registerEntityService(serverEntityService);
    }
    for (Entry<Class<?>, ServiceProvider> entry : this.savedServiceProviders.entrySet()) {
      Class<?> key = entry.getKey();
      ServiceProviderConfiguration configuration = this.savedServiceProviderConfigurations.get(key);
      this.serverProcess.registerServiceProviderForType(key, entry.getValue(), configuration);
    }
    
    // Handle the difference between active restart and passive fail-over.
    // If there was previously a passive, we want to tell it to become active, given this new server as passive, and also
    // hand off all our client connections since they will be reconnecting there, not here.
    PassthroughServer newActive = null;
    if (null == this.savedPassiveServer) {
      // We are the active so just start, loading our storage.
      // Start the server with a reloaded state.
      boolean shouldLoadStorage = true;
      this.serverProcess.start(shouldLoadStorage);
      
      // Reconnect all the connections.
      for(PassthroughConnection connection : this.savedClientConnections.values()) {
        connection.reconnect(this.serverProcess);
      }
      newActive = this;
    } else {
      // Start us WITHOUT loading storage - this is because WE are the PASSIVE, now.
      boolean shouldLoadStorage = false;
      this.serverProcess.start(shouldLoadStorage);
      // We are going to fail-over so tell the other passive to become active before we reconnect clients.
      this.savedPassiveServer.doFailOver(this);
      // The saved passive is now ACTIVE.
      newActive = this.savedPassiveServer;
      this.savedPassiveServer = null;
      // Reconnect all the connections to the new active.
      for(Map.Entry<Long, PassthroughConnection> connection : this.savedClientConnections.entrySet()) {
        newActive.failOverReconnect(connection.getKey(), connection.getValue());
      }
      // Our clients are no longer connected to us so wipe them.
      this.savedClientConnections.clear();
    }
    return newActive;
  }

  private void failOverReconnect(Long connectionID, PassthroughConnection connection) {
    // Save this to our connection list.
    this.savedClientConnections.put(connectionID, connection);
    // Tell the connection to reconnect.
    connection.reconnect(this.serverProcess);
  }

  private void doFailOver(PassthroughServer restartedAsPassive) {
    Assert.assertNull(this.savedPassiveServer);
    this.savedPassiveServer = restartedAsPassive;
    this.serverProcess.promoteToActive();
    // We also want to ask the process to start processing messages as the active.
    this.serverProcess.resumeMessageProcessing();
    // Set the downstream process (has the side-effect of synchronizing all of our entities).
    this.serverProcess.setDownstreamPassiveServerProcess(this.savedPassiveServer.serverProcess);
  }

  private <T> void registerServiceProvider(Class<T> clazz, ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    this.savedServiceProviders.put(clazz, serviceProvider);
    this.savedServiceProviderConfigurations.put(clazz, providerConfiguration);
    this.serverProcess.registerServiceProviderForType(clazz, serviceProvider, providerConfiguration);
  }
}
