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

import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
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
  private final EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse> service;
  private final Class<T> clazz;
  private final long version;
  private final String name;
  
  public PassthroughEntityRef(PassthroughConnection passthroughConnection, EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse> service, Class<T> clazz, long version, String name) {
    this.passthroughConnection = passthroughConnection;
    this.service = service;
    this.clazz = clazz;
    this.version = version;
    this.name = name;
  }

  @Override
  public T fetchEntity() throws EntityNotFoundException, EntityVersionMismatchException {
    long clientInstanceID = this.passthroughConnection.getNewInstanceID();
    PassthroughMessage getMessage = PassthroughMessageCodec.createFetchMessage(this.clazz.getCanonicalName(), this.name, clientInstanceID, this.version);
    PassthroughWait received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
    received.blockGetOnRetire();
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
      // NOTE:  We use a try-lock so that we can emulate the "fast fail" semantics now desired for create() - failure to acquire the lock
      // assumes that the entity already exists.
      byte[] serializedConfiguration = this.service.serializeConfiguration(configuration);
      PassthroughMessage getMessage = PassthroughMessageCodec.createCreateMessage(this.clazz.getCanonicalName(), this.name, this.version, serializedConfiguration);
      PassthroughWait received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
      received.blockGetOnRetire();
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
    } else {
      throw new EntityNotProvidedException(this.clazz.getName(), this.name);
    }
  }
  

  @Override
  public C reconfigure(C configuration) throws EntityException {
    // Make sure that we have a service provider.
    if (null != this.service) {
      try {
        byte[] serializedConfiguration = this.service.serializeConfiguration(configuration);
        PassthroughMessage reconfig = PassthroughMessageCodec.createReconfigureMessage(this.clazz.getCanonicalName(), this.name, this.version, serializedConfiguration);
        PassthroughWait received = this.passthroughConnection.sendInternalMessageAfterAcks(reconfig);
        received.blockGetOnRetire();
        try {
          return this.service.deserializeConfiguration(received.get());
        } catch (EntityException e) {
          // Check that this is the correct type.
          if (e instanceof EntityNotProvidedException) {
            throw (EntityNotProvidedException) e;
          } else if (e instanceof EntityAlreadyExistsException) {
            throw (EntityAlreadyExistsException) e;
          } else if (e instanceof EntityVersionMismatchException) {
            throw (EntityVersionMismatchException) e;
          } else {
            throw e;
          }
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      } finally {
      }
    } else {
      throw new EntityNotProvidedException(this.clazz.getName(), this.name);
    }
  }  

  @Override
  public boolean destroy() throws EntityNotProvidedException, EntityNotFoundException {
    return destroyEntity();
  }

  private boolean destroyEntity() throws EntityNotProvidedException, EntityNotFoundException {
    PassthroughMessage getMessage = PassthroughMessageCodec.createDestroyMessage(this.clazz.getCanonicalName(), this.name);
    PassthroughWait received = this.passthroughConnection.sendInternalMessageAfterAcks(getMessage);
    received.blockGetOnRetire();
    try {
      return received.get()[0] != 0;
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
    return false;
  }
}
