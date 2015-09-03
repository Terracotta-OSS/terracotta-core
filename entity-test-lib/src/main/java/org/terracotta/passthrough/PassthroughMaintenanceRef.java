package org.terracotta.passthrough;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityMaintenanceRef;
import org.terracotta.entity.EntityClientService;


/**
 * Similar to PassthroughEntityRef, although for maintenance operations.  This means that checks of the existence of named
 * entity, as well as creation and destruction of the server-side instance are managed through this object.
 * 
 * @param <T> The entity type
 * @param <C> The configuration type
 */
public class PassthroughMaintenanceRef <T extends Entity, C> implements EntityMaintenanceRef<T, C> {
  private final PassthroughConnection passthroughConnection;
  private final EntityClientService<T, C> service;
  private final long version;
  private final Class<T> entityClass;
  private final String entityName;
  
  public PassthroughMaintenanceRef(PassthroughConnection passthroughConnection, EntityClientService<T, C> service, Class<T> clazz, long version, String name) {
    this.passthroughConnection = passthroughConnection;
    this.service = service;
    this.version = version;
    this.entityClass = clazz;
    this.entityName = name;
    
    // A maintenance ref needs the write-lock on this entity name.
    PassthroughMessage lockMessage = PassthroughMessageCodec.createWriteLockAcquireMessage(this.entityClass, this.entityName);
    try {
      this.passthroughConnection.sendInternalMessageAfterAcks(lockMessage).get();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (ExecutionException e) {
      Assert.unexpected(e);
    }
  }

  @Override
  public boolean doesExist() {
    PassthroughMessage getMessage = PassthroughMessageCodec.createExistsMessage(this.entityClass, this.entityName, this.version);
    Future<byte[]> received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
    boolean doesExist = false;
    try {
      received.get();
      doesExist = true;
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (ExecutionException e) {
      Assert.unexpected(e);
    }
    return doesExist;
  }

  @Override
  public void destroy() {
    PassthroughMessage getMessage = PassthroughMessageCodec.createDestroyMessage(this.entityClass, this.entityName);
    Future<byte[]> received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
    try {
      received.get();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (ExecutionException e) {
      Assert.unexpected(e);
    }
  }

  @Override
  public void create(C configuration) {
    byte[] serializedConfiguration = this.service.serializeConfiguration(configuration);
    PassthroughMessage getMessage = PassthroughMessageCodec.createCreateMessage(this.entityClass, this.entityName, this.version, serializedConfiguration);
    Future<byte[]> received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
    try {
      received.get();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (ExecutionException e) {
      // This means there was actually a problem with the call, which we want to communicate back.
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() {
    // Release our write-lock.
    PassthroughMessage lockMessage = PassthroughMessageCodec.createWriteLockReleaseMessage(this.entityClass, this.entityName);
    try {
      this.passthroughConnection.sendInternalMessageAfterAcks(lockMessage).get();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (ExecutionException e) {
      Assert.unexpected(e);
    }
  }
}
