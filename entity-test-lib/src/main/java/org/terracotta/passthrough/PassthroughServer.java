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
import java.util.Vector;

import org.junit.Assert;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;


/**
 * The top-level "server" of the passthrough testing system.
 * The server can be put into either an "active" or "passive" mode when constructed.  This determines whether server can have
 * downstream passives attached to it.
 */
public class PassthroughServer implements PassthroughDumper {
  private String serverName;
  private int bindPort;
  private int groupPort;
  
  private final boolean isActive;
    
  private PassthroughServerProcess serverProcess;
  private boolean hasStarted;
  private final List<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse>> entityClientServices;
  // Note that we don't currently use the connection ID outside of this class but it is convenient and may be exposed outside, later.
  private long nextConnectionID;
  private PassthroughConnection pseudoConnection;
  
  // We also track various information for the restart case.
  private final List<ServerEntityService<?, ?>> savedServerEntityServices;
  private final List<ServiceProviderAndConfiguration> savedServiceProviderData;
  private final Map<Long, PassthroughConnection> savedClientConnections;
  private PassthroughServer savedPassiveServer;
  
  public PassthroughServer(boolean isActiveMode) {
    this.isActive = isActiveMode;
    this.entityClientServices = new Vector<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse>>();
    this.nextConnectionID = 1;
    
    // Create the containers we will use for tracking the state we will need to repopulate on restart.
    this.savedServerEntityServices = new Vector<ServerEntityService<?, ?>>();
    this.savedServiceProviderData = new Vector<ServiceProviderAndConfiguration>();
    this.savedClientConnections = new HashMap<Long, PassthroughConnection>();
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public void setBindPort(int bindPort) {
    this.bindPort = bindPort;
  }

  public void setGroupPort(int groupPort) {
    this.groupPort = groupPort;
  }
   
  public void registerServerEntityService(ServerEntityService<?, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.savedServerEntityServices.add(service);
  }

  public void registerClientEntityService(EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse> service) {
    Assert.assertFalse(this.hasStarted);
    this.entityClientServices.add(service);
  }

  public synchronized PassthroughConnection connectNewClient() {
    Assert.assertTrue(this.hasStarted);
    final long thisConnectionID = this.nextConnectionID;
    this.nextConnectionID += 1;
    // Note that we need to track the connections for reconnect so pass in this cleanup routine to remove it from our tracking.
    Runnable onClose = new Runnable() {
      @Override
      public void run() {
        synchronized (PassthroughServer.this) {
          PassthroughConnection closedConnection = PassthroughServer.this.savedClientConnections.remove(thisConnectionID);
// connection may have already been closed by a failover
          if (closedConnection != null) {
            PassthroughServer.this.serverProcess.disconnectConnection(closedConnection, thisConnectionID);
          }
        }
      }
    };
    String readerThreadName = "Client connection " + thisConnectionID;
    PassthroughConnection connection = new PassthroughConnection(readerThreadName, this.serverProcess, this.entityClientServices, onClose, thisConnectionID);
    this.serverProcess.connectConnection(connection, thisConnectionID);
    this.savedClientConnections.put(thisConnectionID, connection);
    return connection;
  }

  private PassthroughConnection internalConnectNewPseudoConnection() {
    final long thisConnectionID = this.nextConnectionID;
    this.nextConnectionID += 1;
    // Note that we need to track the connections for reconnect so pass in this cleanup routine to remove it from our tracking.
    Runnable onClose = new Runnable() {
      @Override
      public void run() {
        // We do nothing in this case.
      }
    };
    String readerThreadName = "Pseudo-connection " + thisConnectionID;
    return new PassthroughConnection(readerThreadName, this.serverProcess, this.entityClientServices, onClose, thisConnectionID);
  }

  public void start() {
    // See if there is a monitoring service.
    // XXX: Currently, we do nothing to simulate reconnect after fail-over or restart, which should probably be addressed.
    List<ServiceProvider> providers = new Vector<ServiceProvider>();
    for (ServiceProviderAndConfiguration tuple : this.savedServiceProviderData) {
      providers.add(tuple.serviceProvider);
    }
    
    this.hasStarted = true;
    boolean shouldLoadStorage = false;
    
    bootstrapProcess(this.isActive);
    
    this.serverProcess.start(shouldLoadStorage);
  }
  
  private void bootstrapProcess(boolean active) {
    this.serverProcess = new PassthroughServerProcess(serverName, bindPort, groupPort, active);

    // Populate the server with its services.
    for (ServerEntityService<?, ?> serverEntityService : this.savedServerEntityServices) {
      this.serverProcess.registerEntityService(serverEntityService);
    }
    
    // Create the pseudo-connection, life-cycled within the server, which can be used by services.
    Assert.assertNull(this.pseudoConnection);
    this.pseudoConnection = internalConnectNewPseudoConnection();
    
    // Register built-in services.
    registerBuiltInServices(pseudoConnection);
    // Install the user-created services.
    for (ServiceProviderAndConfiguration tuple : this.savedServiceProviderData) {
      try {
        this.serverProcess.registerServiceProvider(tuple.serviceProvider.getClass().newInstance(), tuple.providerConfiguration);
      } catch (IllegalAccessException a) {
        throw new RuntimeException(a);
      } catch (InstantiationException i) {
        throw new RuntimeException(i);
      }
    }
  }

  public void stop() {
    internalStop();
  }

  private void internalStop() {
    this.serverProcess.shutdown();
    Assert.assertNotNull(this.pseudoConnection);
    this.pseudoConnection.close();
    this.pseudoConnection = null;
  }

  @Deprecated
  // Deprecated in favor of registerServiceProvider (as clazz was unused).
  public <T> void registerServiceProviderForType(Class<T> clazz, ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    internalRegisterServiceProvider(serviceProvider, providerConfiguration);
  }

  public void registerServiceProvider(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    internalRegisterServiceProvider(serviceProvider, providerConfiguration);
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
    internalStop();
    if (null != this.savedPassiveServer) {
      // Now, the passive.
      // NOTE:  The passive has state we want to continue to use so it only partially restarts - not the same as the full
      // "stop" and "bootstrap" sequence in the active. 
      this.savedPassiveServer.serverProcess.shutdown();
    }
    // Start a new one.
    // (we will make it active if it is the only server in the stripe, otherwise it will become passive when we fail-over).
    boolean isActiveMode = (null == this.savedPassiveServer);
  
    bootstrapProcess(isActiveMode);
    
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

  private void internalRegisterServiceProvider(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    this.savedServiceProviderData.add(new ServiceProviderAndConfiguration(serviceProvider, providerConfiguration));
  }

  @Override
  public void dump() {
    System.out.println("Connected passthrough clients:");
    for(PassthroughConnection connection : this.savedClientConnections.values()) {
      System.out.println("\t" + connection);
    }
    this.serverProcess.dump();
  }

  private static class ServiceProviderAndConfiguration {
    public final ServiceProvider serviceProvider;
    public final ServiceProviderConfiguration providerConfiguration;
    
    public ServiceProviderAndConfiguration(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
      this.serviceProvider = serviceProvider;
      this.providerConfiguration = providerConfiguration;
    }
  }

  private void registerBuiltInServices(PassthroughConnection pseudoConnection) {
    PassthroughCommunicatorServiceProvider communicatorServiceProvider = new PassthroughCommunicatorServiceProvider();
    this.serverProcess.registerBuiltInServiceProvider(communicatorServiceProvider, null);
    PassthroughMessengerServiceProvider messengerServiceProvider = new PassthroughMessengerServiceProvider(pseudoConnection);
    this.serverProcess.registerBuiltInServiceProvider(messengerServiceProvider, null);
    PassthroughPlatformServiceProvider passthroughPlatformServiceProvider = new PassthroughPlatformServiceProvider(this);
    this.serverProcess.registerBuiltInServiceProvider(passthroughPlatformServiceProvider, null);
  }
}
