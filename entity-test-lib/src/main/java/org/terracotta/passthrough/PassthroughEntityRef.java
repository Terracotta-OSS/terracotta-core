package org.terracotta.passthrough;

import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientService;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;


/**
 * The client-side object which refers to a specific server-side entity instance.  The client code can call fetchEntity to
 * request a unique client-side instance which back-ends onto this common server-side instance.
 * 
 * TODO:  Fetched entities do not yet hold a read-lock on the server-side entity.
 * 
 * @param <T> The entity type
 * @param <C> The configuration type
 */
public class PassthroughEntityRef<T extends Entity, C> implements EntityRef<T, C> {
  private final PassthroughConnection passthroughConnection;
  private final EntityClientService<T, C> service;
  private final Class<T> clazz;
  private final long version;
  private final String name;
  
  public PassthroughEntityRef(PassthroughConnection passthroughConnection, EntityClientService<T, C> service, Class<T> clazz, long version, String name) {
    this.passthroughConnection = passthroughConnection;
    this.service = service;
    this.clazz = clazz;
    this.version = version;
    this.name = name;
  }

  @Override
  public T fetchEntity() throws EntityNotFoundException, EntityVersionMismatchException {
    long clientInstanceID = this.passthroughConnection.getNewInstanceID();
    PassthroughMessage getMessage = PassthroughMessageCodec.createFetchMessage(this.clazz, this.name, clientInstanceID, this.version);
    PassthroughWait received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
    // Wait for the config on the response.
    byte[] rawConfig = null;
    try {
      rawConfig = received.get();
    } catch (EntityException e) {
      // Check that this is the correct type.
      if (e instanceof EntityNotFoundException) {
        throw (EntityNotFoundException) e;
      } else if (e instanceof EntityVersionMismatchException) {
        throw (EntityVersionMismatchException) e;
      } else {
        Assert.unexpected(e);
      }
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    }
    return this.passthroughConnection.createEntityInstance(this.clazz, this.name, clientInstanceID, this.version, rawConfig);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void create(C configuration) throws EntityNotProvidedException, EntityAlreadyExistsException, EntityVersionMismatchException {
    // Make sure that we have a service provider.
    if (null != this.service) {
      getWriteLock();
      try {
        byte[] serializedConfiguration = this.service.serializeConfiguration(configuration);
        PassthroughMessage getMessage = PassthroughMessageCodec.createCreateMessage(this.clazz.getCanonicalName(), this.name, this.version, serializedConfiguration);
        PassthroughWait received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
        try {
          received.get();
        } catch (EntityException e) {
          // Check that this is the correct type.
          if (e instanceof EntityNotProvidedException) {
            throw (EntityNotProvidedException) e;
          } else if (e instanceof EntityAlreadyExistsException) {
            throw (EntityAlreadyExistsException) e;
          } else if (e instanceof EntityVersionMismatchException) {
            throw (EntityVersionMismatchException) e;
          } else {
            Assert.unexpected(e);
          }
        } catch (InterruptedException e) {
          Assert.unexpected(e);
        }
      } finally {
        releaseWriteLock();
      }
    } else {
      throw new EntityNotProvidedException(this.clazz.getName(), this.name);
    }
  }

  @Override
  public void destroy() throws EntityNotProvidedException, EntityNotFoundException {
    getWriteLock();
    try {
      PassthroughMessage getMessage = PassthroughMessageCodec.createDestroyMessage(this.clazz, this.name);
      PassthroughWait received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
      try {
        received.get();
      } catch (EntityException e) {
        // Check that this is the correct type.
        if (e instanceof EntityNotProvidedException) {
          throw (EntityNotProvidedException) e;
        } else if (e instanceof EntityNotFoundException) {
          throw (EntityNotFoundException) e;
        } else {
          Assert.unexpected(e);
        }
      } catch (InterruptedException e) {
        Assert.unexpected(e);
      }
    } finally {
      releaseWriteLock();
    }
  }

  private void getWriteLock() {
    PassthroughMessage lockMessage = PassthroughMessageCodec.createWriteLockAcquireMessage(this.clazz, this.name);
    try {
      this.passthroughConnection.sendInternalMessageAfterAcks(lockMessage).get();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (EntityException e) {
      Assert.unexpected(e);
    }
  }

  private void releaseWriteLock() {
    PassthroughMessage lockMessage = PassthroughMessageCodec.createWriteLockReleaseMessage(this.clazz, this.name);
    try {
      this.passthroughConnection.sendInternalMessageAfterAcks(lockMessage).get();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    } catch (EntityException e) {
      Assert.unexpected(e);
    }
  }
}
