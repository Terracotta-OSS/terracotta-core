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

import java.util.concurrent.ExecutionException;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
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
 * @param <U> User data type
 */
public class PassthroughEntityRef<T extends Entity, C, U> implements EntityRef<T, C, U> {
  private final PassthroughConnection passthroughConnection;
  private final EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> service;
  private final String clazz;
  private final long version;
  private final String name;
  
  public PassthroughEntityRef(PassthroughConnection passthroughConnection, EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> service, String clazz, long version, String name) {
    this.passthroughConnection = passthroughConnection;
    this.service = service;
    this.clazz = clazz;
    this.version = version;
    this.name = name;
  }

  @Override
  public boolean isValid() {
    return passthroughConnection.isValid();
  }

  @Override
  public T fetchEntity(U userData) throws EntityNotFoundException, EntityVersionMismatchException {
    long clientInstanceID = this.passthroughConnection.getNewInstanceID();
    PassthroughMessage getMessage = PassthroughMessageCodec.createFetchMessage(this.clazz, this.name, clientInstanceID, this.version);
    // Wait for the config on the response.
    byte[] rawConfig = null;
    try {
      rawConfig = passthroughConnection.invokeAndRetire(getMessage);

    } catch (ExecutionException b) {
      EntityException e = (EntityException)b.getCause();
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
    try {
      Class<?> type = Class.forName(this.clazz);
      return this.passthroughConnection.createEntityInstance(type, this.name, clientInstanceID, this.version, rawConfig, userData);
    } catch (ClassNotFoundException notfound) {
      throw new EntityNotFoundException(clazz, name, notfound);
    }
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void create(C configuration) throws EntityNotProvidedException, EntityAlreadyExistsException, EntityVersionMismatchException, EntityConfigurationException {
    // Make sure that we have a service provider.
    if (null != this.service) {
      // NOTE:  We use a try-lock so that we can emulate the "fast fail" semantics now desired for create() - failure to acquire the lock
      // assumes that the entity already exists.
      byte[] serializedConfiguration = this.service.serializeConfiguration(configuration);
      PassthroughMessage getMessage = PassthroughMessageCodec.createCreateMessage(this.clazz, this.name, this.version, serializedConfiguration);
      try {
        passthroughConnection.invokeAndRetire(getMessage);
      } catch (ExecutionException b) {
        EntityException e = (EntityException)b.getCause();
        // Check that this is the correct type.
        if (e instanceof EntityNotProvidedException) {
          throw (EntityNotProvidedException) e;
        } else if (e instanceof EntityAlreadyExistsException) {
          throw (EntityAlreadyExistsException) e;
        } else if (e instanceof EntityVersionMismatchException) {
          throw (EntityVersionMismatchException) e;
        } else if (e instanceof EntityConfigurationException) {
          throw (EntityConfigurationException) e;
        } else {
          Assert.unexpected(e);
        }
      } catch (InterruptedException e) {
        Assert.unexpected(e);
      }
    } else {
      throw new EntityNotProvidedException(this.clazz, this.name);
    }
  }
  

  @Override
  public C reconfigure(C configuration) throws EntityNotProvidedException, EntityNotFoundException, EntityConfigurationException {
    C result = null;
    // Make sure that we have a service provider.
    if (null != this.service) {
      byte[] serializedConfiguration = this.service.serializeConfiguration(configuration);
      PassthroughMessage reconfig = PassthroughMessageCodec.createReconfigureMessage(this.clazz, this.name, this.version, serializedConfiguration);
      try {
        result = this.service.deserializeConfiguration(passthroughConnection.invokeAndRetire(reconfig));
      } catch (ExecutionException b) {
        EntityException e = (EntityException)b.getCause();        // Check that this is the correct type.
        if (e instanceof EntityNotFoundException) {
          throw (EntityNotFoundException) e;
        } else if (e instanceof EntityNotProvidedException) {
          throw (EntityNotProvidedException) e;
        } else if (e instanceof EntityConfigurationException) {
          throw (EntityConfigurationException) e;
        } else {
          // This isn't a supported entity type.
          Assert.unexpected(e);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new EntityNotProvidedException(this.clazz, this.name);
    }
    return result;
  }

  @Override
  public boolean destroy() throws EntityNotProvidedException, EntityNotFoundException {
    return destroyEntity();
  }

  private boolean destroyEntity() throws EntityNotProvidedException, EntityNotFoundException {
    PassthroughMessage getMessage = PassthroughMessageCodec.createDestroyMessage(this.clazz, this.name);
    try {
      return passthroughConnection.invokeAndRetire(getMessage)[0] != 0;
    } catch (ExecutionException b) {
      EntityException e = (EntityException)b.getCause();      // Check that this is the correct type.
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
