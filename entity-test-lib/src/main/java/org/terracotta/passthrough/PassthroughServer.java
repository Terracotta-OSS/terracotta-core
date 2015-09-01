package org.terracotta.passthrough;

import java.util.List;
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
  
  public PassthroughServer(boolean isActiveMode) {
    this.serverProcess = new PassthroughServerProcess(isActiveMode);
    this.entityClientServices = new Vector<>();
    
    // Register built-in services.
    PassthroughCommunicatorServiceProvider communicatorServiceProvider = new PassthroughCommunicatorServiceProvider();
    this.serverProcess.registerServiceProviderForType(ClientCommunicator.class, communicatorServiceProvider);
  }

  public void registerServerEntityService(ServerEntityService<?, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.serverProcess.registerEntityService(service);
  }

  public void registerClientEntityService(EntityClientService<?, ?> service) {
    Assert.assertFalse(this.hasStarted);
    this.entityClientServices.add(service);
  }

  public Connection connectNewClient() {
    Assert.assertTrue(this.hasStarted);
    return new PassthroughConnection(this.serverProcess, this.entityClientServices);
  }

  public void start() {
    this.hasStarted = true;
    this.serverProcess.start();
  }

  public void stop() {
    this.serverProcess.shutdown();
  }

  public <T> void registerServiceProviderForType(Class<T> clazz, ServiceProvider serviceProvider) {
    this.serverProcess.registerServiceProviderForType(clazz, serviceProvider);
  }

  public void attachDownstreamPassive(PassthroughServer passiveServer) {
    this.serverProcess.setDownstreamPassiveServerProcess(passiveServer.serverProcess);
  }
}
