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

import com.tc.classloader.BuiltinService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.PlatformConfiguration;
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
  // Each connection needs a unique ID for internal tracking purposes but the ID is expected to be global, across all server
  //  instances, so we will statically assign them.
  private static final AtomicLong nextConnectionID = new AtomicLong(0L);


  private String serverName;
  private int bindPort;
  private int groupPort;
  
  private boolean isActive;
    
  private PassthroughServerProcess serverProcess;
  private boolean hasStarted;
  private final List<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse>> entityClientServices;
  private PassthroughConnection pseudoConnection;
  private PassthroughMonitoringProducer monitoringProducer;
  
  // We also track various information for the restart case.
  private final List<EntityServerService<?, ?>> savedServerEntityServices;
  private final List<ServiceProviderAndConfiguration> savedServiceProviderData;
  private final Map<Long, PassthroughConnection> savedClientConnections;
  
  public PassthroughServer() {
    this.entityClientServices = new Vector<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse>>();
    
    // Create the containers we will use for tracking the state we will need to repopulate on restart.
    this.savedServerEntityServices = new Vector<EntityServerService<?, ?>>();
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
   
  public void registerServerEntityService(EntityServerService<?, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.savedServerEntityServices.add(service);
  }

  public void registerClientEntityService(EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse> service) {
    Assert.assertFalse(this.hasStarted);
    this.entityClientServices.add(service);
  }

  public synchronized PassthroughConnection connectNewClient(String connectionName) {
    Assert.assertTrue(this.hasStarted);
    final long thisConnectionID = nextConnectionID.incrementAndGet();
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
    PassthroughConnection connection = new PassthroughConnection(connectionName, readerThreadName, this.serverProcess, this.entityClientServices, onClose, thisConnectionID);
    this.serverProcess.connectConnection(connection, thisConnectionID);
    this.savedClientConnections.put(thisConnectionID, connection);
    return connection;
  }

  private PassthroughConnection internalConnectNewPseudoConnection() {
    final long thisConnectionID = nextConnectionID.incrementAndGet();
    // Note that we need to track the connections for reconnect so pass in this cleanup routine to remove it from our tracking.
    Runnable onClose = new Runnable() {
      @Override
      public void run() {
        // We do nothing in this case.
      }
    };
    String readerThreadName = "Pseudo-connection " + thisConnectionID;
    return new PassthroughConnection("internal pseudo-connection", readerThreadName, this.serverProcess, this.entityClientServices, onClose, thisConnectionID);
  }

  public void start(boolean isActive, boolean shouldLoadStorage) {
    this.isActive = isActive;
    // See if there is a monitoring service.
    // XXX: Currently, we do nothing to simulate reconnect after fail-over or restart, which should probably be addressed.
    List<ServiceProvider> providers = new Vector<ServiceProvider>();
    for (ServiceProviderAndConfiguration tuple : this.savedServiceProviderData) {
      providers.add(tuple.serviceProvider);
    }
    this.hasStarted = true;
    bootstrapProcess(this.isActive);
    this.serverProcess.start(shouldLoadStorage);
    
    // If we are active, tell the monitoring system.
    if (this.isActive) {
      this.monitoringProducer.didBecomeActive(this.serverProcess.getServerInfo());
    }
  }
  
  private void bootstrapProcess(boolean active) {
    this.serverProcess = new PassthroughServerProcess(serverName, bindPort, groupPort, active);

    // Populate the server with its services.
    for (EntityServerService<?, ?> serverEntityService : this.savedServerEntityServices) {
      this.serverProcess.registerEntityService(serverEntityService);
    }
    
    // Create the pseudo-connection, life-cycled within the server, which can be used by services.
    Assert.assertNull(this.pseudoConnection);
    this.pseudoConnection = internalConnectNewPseudoConnection();
    
    // Register built-in services.
    registerBuiltInServices(pseudoConnection);
    
    findClasspathBuiltinServices();
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
    this.serverProcess.stop();
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
    this.serverProcess.addDownstreamPassiveServerProcess(passiveServer.serverProcess);
  }

  public void detachDownstreamPassive(PassthroughServer passiveServer) {
    this.serverProcess.removeDownstreamPassiveServerProcess(passiveServer.serverProcess);
  }

  public void connectSavedClientsTo(PassthroughServer newActive) {
    for(Map.Entry<Long, PassthroughConnection> connection : this.savedClientConnections.entrySet()) {
      newActive.failOverReconnect(connection.getKey(), connection.getValue());
    }
    newActive.serverProcess.beginReceivingResends();
    for(Map.Entry<Long, PassthroughConnection> connection : this.savedClientConnections.entrySet()) {
      connection.getValue().finishReconnect();
    }
    newActive.serverProcess.endReceivingResends();

    if(!this.isActive) {
      this.savedClientConnections.clear();
    }
  }

  public void disconnectClients() {
    for (PassthroughConnection passthroughConnection : savedClientConnections.values()) {
      passthroughConnection.disconnect();
    }
  }

  private void failOverReconnect(Long connectionID, PassthroughConnection connection) {
    // Save this to our connection list.
    this.savedClientConnections.put(connectionID, connection);
    // Tell the connection to reconnect.
    connection.startReconnect(this.serverProcess);
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

  public void promoteToActive() {
    this.isActive = true;
    this.serverProcess.stop();
    // Tell the monitoring producer that we became active.
    this.monitoringProducer.didBecomeActive(this.serverProcess.getServerInfo());
    this.serverProcess.promoteToActive();
    this.serverProcess.resumeMessageProcessing();
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
    this.serverProcess.registerImplementationProvidedServiceProvider(communicatorServiceProvider, null);
    PassthroughMessengerServiceProvider messengerServiceProvider = new PassthroughMessengerServiceProvider(this.serverProcess, pseudoConnection);
    this.serverProcess.registerImplementationProvidedServiceProvider(messengerServiceProvider, null);
    PassthroughPlatformServiceProvider passthroughPlatformServiceProvider = new PassthroughPlatformServiceProvider(this);
    this.serverProcess.registerImplementationProvidedServiceProvider(passthroughPlatformServiceProvider, null);
    this.monitoringProducer = new PassthroughMonitoringProducer(this.serverProcess);
    this.serverProcess.registerImplementationProvidedServiceProvider(this.monitoringProducer, null);
  }
  
  private void findClasspathBuiltinServices() {
      java.util.ServiceLoader<ServiceProvider> loader = ServiceLoader.load(ServiceProvider.class);
      for (ServiceProvider provider : loader) {
        if (!provider.getClass().isAnnotationPresent(BuiltinService.class)) {
          System.err.println("service:" + provider.getClass().getName() + " not annotated with @BuiltinService.  The service will not be included");
        } else {
          // We want to initialize built-in providers with a null configuration.
          provider.initialize(null, new PlatformConfiguration() {
            @Override
            public String getServerName() {
              return serverName;
            }
          });
          this.serverProcess.registerServiceProvider(provider, null);
        }
      }
  }
}
