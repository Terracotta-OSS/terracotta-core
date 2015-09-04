package org.terracotta.passthrough;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.Assert;
import org.terracotta.connection.Connection;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceProvider;


/**
 * The top-level "server" of the passthrough testing system.
 * The server can be put into either an "active" or "passive" mode when constructed.  This determines whether server can have
 * downstream passives attached to it.
 */
public class PassthroughServer {
  private final PassthroughServerProcess serverProcess;
  private boolean hasStarted;
  private final List<EntityClientService<?, ?>> entityClientServices;
  // Note that we don't currently use the connection ID outside of this class but it is convenient and may be exposed outside, later.
  private long nextConnectionID;
  
  // We also track various information for the restart case.
  private final List<ServerEntityService<?, ?>> savedServerEntityServices;
  private final Map<Class<?>, ServiceProvider> savedServiceProviders;
  private final Map<Long, PassthroughConnection> savedClientConnections;
  @SuppressWarnings("unused")
  private PassthroughServer savedPassiveServer;
  
  public PassthroughServer(boolean isActiveMode) {
    this.serverProcess = new PassthroughServerProcess(isActiveMode);
    this.entityClientServices = new Vector<EntityClientService<?, ?>>();
    this.nextConnectionID = 1;
    
    // Create the containers we will use for tracking the state we will need to repopulate on restart.
    this.savedServerEntityServices = new Vector<ServerEntityService<?, ?>>();
    this.savedServiceProviders = new HashMap<Class<?>, ServiceProvider>();
    this.savedClientConnections = new HashMap<Long, PassthroughConnection>();
    
    // Register built-in services.
    PassthroughCommunicatorServiceProvider communicatorServiceProvider = new PassthroughCommunicatorServiceProvider();
    registerServiceProvider(ClientCommunicator.class, communicatorServiceProvider);
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

  public Connection connectNewClient() {
    Assert.assertTrue(this.hasStarted);
    final long thisConnectionID = this.nextConnectionID;
    this.nextConnectionID += 1;
    // Note that we need to track the connections for reconnect so pass in this cleanup routine to remove it from our tracking.
    Runnable onClose = new Runnable() {
      @Override
      public void run() {
        PassthroughServer.this.savedClientConnections.remove(thisConnectionID);
      }
    };
    PassthroughConnection connection = new PassthroughConnection(this.serverProcess, this.entityClientServices, onClose);
    this.savedClientConnections.put(thisConnectionID, connection);
    return connection;
  }

  public void start() {
    this.hasStarted = true;
    this.serverProcess.start();
  }

  public void stop() {
    this.serverProcess.shutdown();
  }

  public <T> void registerServiceProviderForType(Class<T> clazz, ServiceProvider serviceProvider) {
    registerServiceProvider(clazz, serviceProvider);
  }

  public void attachDownstreamPassive(PassthroughServer passiveServer) {
    this.savedPassiveServer = passiveServer;
    this.serverProcess.setDownstreamPassiveServerProcess(passiveServer.serverProcess);
  }

  private <T> void registerServiceProvider(Class<T> clazz, ServiceProvider serviceProvider) {
    this.savedServiceProviders.put(clazz, serviceProvider);
    this.serverProcess.registerServiceProviderForType(clazz, serviceProvider);
  }
}
