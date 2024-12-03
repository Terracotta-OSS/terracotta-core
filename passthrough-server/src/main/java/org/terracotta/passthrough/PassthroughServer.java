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

import com.tc.classloader.BuiltinService;
import com.tc.classloader.OverrideService;
import com.tc.classloader.OverrideServiceType;
import com.tc.classloader.PermanentEntity;
import com.tc.classloader.PermanentEntityType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.exception.EntityException;


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
  private final List<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?>> entityClientServices;
  private PassthroughMonitoringProducer monitoringProducer;
  
  private IAsynchronousServerCrasher crasher;
  
  // We also track various information for the restart case.
  private final List<EntityServerService<?, ?>> savedServerEntityServices;
  private final List<ServiceProviderAndConfiguration> savedServiceProviderData;
  private final Collection<Object> extendedConfigurationObjects;
  private final Map<Long, PassthroughConnection> savedClientConnections;
  private PassthroughClusterControl passthroughClusterControl;

  public PassthroughServer() {
    this.entityClientServices = new Vector<EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?>>();
    
    // Create the containers we will use for tracking the state we will need to repopulate on restart.
    this.savedServerEntityServices = new Vector<EntityServerService<?, ?>>();
    this.savedServiceProviderData = new Vector<ServiceProviderAndConfiguration>();
    this.extendedConfigurationObjects = new Vector<Object>();
    this.savedClientConnections = new HashMap<Long, PassthroughConnection>();
  }

  public void registerAsynchronousServerCrasher(IAsynchronousServerCrasher crasher) {
    // This should only be set once.
    Assert.assertNull(this.crasher);
    this.crasher = crasher;
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

  public void registerClientEntityService(EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.entityClientServices.add(service);
  }

  public PassthroughConnection connectNewClient(String connectionName) {
    return connectNewClient(connectionName, new PassthroughEndpointConnectorImpl());
  }

  public synchronized PassthroughConnection connectNewClient(String connectionName, PassthroughEndpointConnector endpointConnector) {
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
    PassthroughConnection connection = new PassthroughConnection(connectionName, readerThreadName, this.serverProcess, this.entityClientServices, onClose, thisConnectionID, endpointConnector);
    connection.startProcessingRequests();
    this.serverProcess.connectConnection(connection, thisConnectionID);
    this.savedClientConnections.put(thisConnectionID, connection);
    return connection;
  }

  public void start(boolean isActive, boolean shouldLoadStorage) {
    start(isActive, shouldLoadStorage, Collections.<Long>emptySet());
  }

  public void start(boolean isActive, boolean shouldLoadStorage, Set<Long> savedClientConnections) {
    this.isActive = isActive;
    
    this.hasStarted = true;
    bootstrapProcess(this.isActive);
    this.serverProcess.start(shouldLoadStorage, savedClientConnections);
    
    // If we are active, tell the monitoring system.
    if (this.isActive) {
      if (!shouldLoadStorage) {
        addPermanentEntities();
      }
      this.monitoringProducer.didBecomeActive(this.serverProcess.getServerInfo());
    }
  }

  @SuppressWarnings("unchecked")
  private void addPermanentEntities() {
    // Populate the server with its services.
    for (EntityServerService<?,?> serverEntityService : this.savedServerEntityServices) {
      try {
        for (PermanentEntity p : serverEntityService.getClass().getAnnotationsByType(PermanentEntity.class)) {
          serverProcess.create(p.type(), p.name(), p.version(), new byte[0]);
        }
        for (PermanentEntityType p : serverEntityService.getClass().getAnnotationsByType(PermanentEntityType.class)) {
          serverProcess.create(p.type().getName(), p.name(), p.version(), new byte[0]);
        }
      } catch (EntityException exp) {
        throw new RuntimeException(exp);
      }
    }
  }

  private void bootstrapProcess(boolean active) {
    this.serverProcess = new PassthroughServerProcess(serverName, bindPort, groupPort, this.extendedConfigurationObjects, active, this.crasher);

    // Populate the server with its services.
    for (EntityServerService<?, ?> serverEntityService : this.savedServerEntityServices) {
      this.serverProcess.registerEntityService(serverEntityService);
    }
    
    // Register built-in services.
    registerImplementationProvidedServices();
    
    findClasspathBuiltinServices();

    // Install the user-created services.
    internalInstallServiceProvider();
    //  if this is the active, try and disconnect the clients
    this.serverProcess.setCrashHandler((Thread t, Throwable e) -> {
      System.err.println("FATAL EXCEPTION IN PASSTHROUGH SERVER THREAD: " + t);
      e.printStackTrace();
      if (isActive) {
        disconnectClients();
      }
    });
  }

  private void internalInstallServiceProvider() {
      for (ServiceProviderAndConfiguration tuple : savedServiceProviderData) {
      try {
          serverProcess.registerServiceProvider(tuple.serviceProvider.getClass().newInstance(), tuple.providerConfiguration);
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
    this.serverProcess.shutdownServices();
    this.serverProcess.stop();
    this.monitoringProducer.serverDidStop();
  }

  @Deprecated
  // Deprecated in favor of registerServiceProvider (as clazz was unused).
  public <T> void registerServiceProviderForType(Class<T> clazz, ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    internalRegisterServiceProvider(serviceProvider, providerConfiguration);
  }

  public void registerServiceProvider(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    internalRegisterServiceProvider(serviceProvider, providerConfiguration);
  }

  /**
   *
   * NOTE: this needs to be deprecated, it provides distinctly different semantics from the clustered version 
   * of the same
   */
  public void registerOverrideServiceProvider(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    internalRegisterServiceProviderAsOverride(serviceProvider, providerConfiguration);
  }

  public void registerExtendedConfiguration(Object extendedConfigObject) {
    this.extendedConfigurationObjects.add(extendedConfigObject);
  }

  public void attachDownstreamPassive(PassthroughServer passiveServer) {
    // Before we attach the downstream to the server process, thus causing the sync, we need to attach the monitoring
    //  producer, since it needs to know where to forward data to the upstream active.
    // NOTE:  This will currently call directly since it simplifies the message flow, for now, but this will likely change
    //  once we have a better sense of how to organize these message channels between the passthrough server processes.
    passiveServer.monitoringProducer.setUpstreamActive(this.monitoringProducer, passiveServer.serverProcess.getServerInfo());
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

  private void internalRegisterServiceProviderAsOverride(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
    this.savedServiceProviderData.add(new ServiceProviderAndConfiguration(serviceProvider, providerConfiguration, true));
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

  public boolean isRunningProcess(PassthroughServerProcess victim) {
    return (this.serverProcess == victim);
  }

  public Set<Long> getSavedClientConnections() {
    return savedClientConnections != null ? Collections.unmodifiableSet(savedClientConnections.keySet()) : Collections.<Long>emptySet();
  }

  public void setClusterControl(PassthroughClusterControl passthroughClusterControl) {
    this.passthroughClusterControl = passthroughClusterControl;
  }

  private static class ServiceProviderAndConfiguration {
    public final ServiceProvider serviceProvider;
    public final ServiceProviderConfiguration providerConfiguration;
    public final boolean override;
    
    public ServiceProviderAndConfiguration(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration) {
      this.serviceProvider = serviceProvider;
      this.providerConfiguration = providerConfiguration;
      this.override = false;
    }
    
    public ServiceProviderAndConfiguration(ServiceProvider serviceProvider, ServiceProviderConfiguration providerConfiguration, boolean override) {
      this.serviceProvider = serviceProvider;
      this.providerConfiguration = providerConfiguration;
      this.override = override;
    }
  }

  private void registerImplementationProvidedServices() {
    PassthroughCommunicatorServiceProvider communicatorServiceProvider = new PassthroughCommunicatorServiceProvider();
    this.serverProcess.registerImplementationProvidedServiceProvider(communicatorServiceProvider, null);
    PassthroughMessengerServiceProvider messengerServiceProvider = new PassthroughMessengerServiceProvider(this.serverProcess);
    this.serverProcess.registerImplementationProvidedServiceProvider(messengerServiceProvider, null);
    PassthroughPlatformServiceProvider passthroughPlatformServiceProvider = new PassthroughPlatformServiceProvider(passthroughClusterControl, this);
    this.serverProcess.registerImplementationProvidedServiceProvider(passthroughPlatformServiceProvider, null);
    this.monitoringProducer = new PassthroughMonitoringProducer(this.serverProcess);
    this.serverProcess.registerImplementationProvidedServiceProvider(this.monitoringProducer, null);
  }
  
  private void findClasspathBuiltinServices() {
    Map<String, Class<? extends ServiceProvider>> overrides = new HashMap<>();
    Map<String, ServiceProvider> providers = new HashMap<>();
    java.util.ServiceLoader<ServiceProvider> loader = ServiceLoader.load(ServiceProvider.class);
    for (ServiceProvider provider : loader) {
      if (!provider.getClass().isAnnotationPresent(BuiltinService.class)) {
        System.err.println("service:" + provider.getClass().getName() + " not annotated with @BuiltinService.  The service will not be included");
      } else {
        // We want to initialize built-in providers with a null configuration only if the test 
        // has not preinstalled an override provider with the existing types
        if (!hasConfigurationForServiceProvider(provider) && !hasOverrideProviderForTypes(provider)) {
          Class<? extends ServiceProvider> type = provider.getClass();
          if (type.isAnnotationPresent(OverrideService.class)) {
            for (OverrideService override : type.getAnnotationsByType(OverrideService.class)) {
              String value = override.value();
              String[] types = override.types();
              if (value != null && value.length() > 0) {
                providers.remove(value);
                overrides.put(value, type);
              }
              for (String typeName : types) {
                providers.remove(typeName);
                overrides.put(typeName, type);
              }
            }
          }
          if (type.isAnnotationPresent(OverrideServiceType.class)) {
            for (OverrideServiceType override : type.getAnnotationsByType(OverrideServiceType.class)) {
              Class<?> value = override.value();
              if (value != null) {
                providers.remove(value.getName());
                overrides.put(value.getName(), type);
              }
            }
          }
          if (!overrides.containsKey(type.getName())) {
            providers.put(type.getName(), provider);
          }
        }
      }
    }
    
    providers.values().forEach((p) -> {
      this.serverProcess.registerServiceProvider(p, null);
    });
  }
  
  private boolean hasOverrideProviderForTypes(ServiceProvider provider) {
    Collection<Class<?>> types = provider.getProvidedServiceTypes();
    for (ServiceProviderAndConfiguration configuredServiceProvider : savedServiceProviderData) {
      if (configuredServiceProvider.override) {
        Collection<Class<?>> existingTypes = configuredServiceProvider.serviceProvider.getProvidedServiceTypes();
        if (existingTypes.containsAll(types)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean hasConfigurationForServiceProvider(ServiceProvider provider) {
    Class<? extends ServiceProvider> providerClass = provider.getClass();
    for (ServiceProviderAndConfiguration configuredServiceProvider : savedServiceProviderData) {
      Class<? extends ServiceProvider> existingServiceProviderClass = configuredServiceProvider.serviceProvider.getClass();
      if (existingServiceProviderClass.equals(providerClass)) {
        return true;
      }
    }

    return false;
  }
}
