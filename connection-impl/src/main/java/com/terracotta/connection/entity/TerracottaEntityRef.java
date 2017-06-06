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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.terracotta.connection.entity;

import com.tc.object.ExceptionUtils;
import com.terracotta.connection.EndpointConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.exception.PermanentEntityException;

import com.tc.object.ClientEntityManager;
import com.tc.object.ClientEntityManagerImpl;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityID;
import com.tc.util.Assert;
import com.tc.util.Throwables;
import com.tc.util.Util;

import java.util.concurrent.atomic.AtomicLong;


public class TerracottaEntityRef<T extends Entity, C, U> implements EntityRef<T, C, U> {
  private final static Logger logger = LoggerFactory.getLogger(TerracottaEntityRef.class);
  private final ClientEntityManager entityManager;
  private final EndpointConnector endpointConnector;
  private final Class<T> type;
  private final long version;
  private final String name;
  private final EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> entityClientService;

  // Each instance fetched by this ref can be individually addressed by the server so it needs a unique ID.
  private final AtomicLong nextClientInstanceID;

  public TerracottaEntityRef(ClientEntityManager entityManager, EndpointConnector endpointConnector,
                             Class<T> type, long version, String name, EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> entityClientService,
                             AtomicLong clientIds) {
    this.entityManager = entityManager;
    this.endpointConnector = endpointConnector;
    this.type = type;
    this.version = version;
    this.name = name;
    this.entityClientService = entityClientService;
    this.nextClientInstanceID = clientIds;
  }
  
  public boolean wasBusy() {
    return ((ClientEntityManagerImpl)entityManager).checkBusy();
  }

  @Override
  public synchronized T fetchEntity(U userData) throws EntityNotFoundException, EntityVersionMismatchException {
    EntityClientEndpoint endpoint = null;
    try {
      final ClientInstanceID clientInstanceID = new ClientInstanceID(this.nextClientInstanceID.getAndIncrement());
      endpoint = entityManager.fetchEntity(this.getEntityID(), this.version, clientInstanceID, entityClientService.getMessageCodec(), null);
    } catch (EntityException e) {
      // In this case, we want to close the endpoint but still throw back the exception.
      // Note that we must externally only present the specific exception types we were expecting.  Thus, we need to check
      // that this is one of those supported types, asserting that there was an unexpected wire inconsistency, otherwise.
      if (e instanceof EntityNotFoundException) {
        throw (EntityNotFoundException)e;
      } else if (e instanceof EntityVersionMismatchException) {
        throw (EntityVersionMismatchException)e;
      } else {
        throw Assert.failure("Unsupported exception type returned to fetch", e);
      }
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
    
    // Note that a failure to resolve the endpoint would have thrown so this can't be null.
    if (endpoint == null) {
      Assert.assertNotNull(endpoint);
    }

    return (T) endpointConnector.connect(endpoint, entityClientService, userData);
  }

  @Override
  public String getName() {
    return name;
  }

  private EntityID getEntityID() {
    return new EntityID(type.getName(), name);
  }

  @Override
  public void create(final C configuration) throws EntityNotProvidedException, EntityAlreadyExistsException, EntityVersionMismatchException, EntityConfigurationException {
    final EntityID entityID = getEntityID();
    try {
      entityManager.createEntity(entityID, version, entityClientService.serializeConfiguration(configuration));
    } catch (EntityException e) {
      // Note that we must externally only present the specific exception types we were expecting.  Thus, we need to check
      // that this is one of those supported types, asserting that there was an unexpected wire inconsistency, otherwise.
      e = ExceptionUtils.addLocalStackTraceToEntityException(e);
      if (e instanceof EntityNotProvidedException) {
        throw (EntityNotProvidedException)e;
      } else if (e instanceof EntityAlreadyExistsException) {
        throw (EntityAlreadyExistsException)e;
      } else if (e instanceof EntityVersionMismatchException) {
        throw (EntityVersionMismatchException)e;
      } else if (e instanceof EntityConfigurationException) {
        throw (EntityConfigurationException) e;
      } else {
        // WARNING:  Assert.failure returns an exception, instead of throwing one.
        throw Assert.failure("Unsupported exception type returned to create", e);
      }
    }
  }

  @Override
  public C reconfigure(final C configuration) throws EntityNotProvidedException, EntityNotFoundException, EntityConfigurationException {
    final EntityID entityID = getEntityID();
    try {
      return entityClientService.deserializeConfiguration(
            entityManager.reconfigureEntity(entityID, version, entityClientService.serializeConfiguration(configuration)));
    } catch (EntityException e) {
      // Note that we must externally only present the specific exception types we were expecting.  Thus, we need to check
      // that this is one of those supported types, asserting that there was an unexpected wire inconsistency, otherwise.
      e = ExceptionUtils.addLocalStackTraceToEntityException(e);
      if (e instanceof EntityNotFoundException) {
        throw (EntityNotFoundException)e;
      } else if (e instanceof EntityNotProvidedException) {
        throw (EntityNotProvidedException)e;
      } else if (e instanceof EntityConfigurationException) {
        throw (EntityConfigurationException) e;
      } else {
        // WARNING:  Assert.failure returns an exception, instead of throwing one.
        throw Assert.failure("Unsupported exception type returned to reconfigure", e);
      }
    }
  }
  
  @Override
  public boolean destroy() throws EntityNotProvidedException, EntityNotFoundException, PermanentEntityException {
    EntityID entityID = getEntityID();
    
    try {
      return this.entityManager.destroyEntity(entityID, this.version);
    } catch (EntityException e) {
      // Note that we must externally only present the specific exception types we were expecting.  Thus, we need to check
      // that this is one of those supported types, asserting that there was an unexpected wire inconsistency, otherwise.
      // NOTE: PermanentEntityException is thrown by this method.
      e = ExceptionUtils.addLocalStackTraceToEntityException(e);
      if (e instanceof EntityNotProvidedException) {
        throw (EntityNotProvidedException)e;
      } else if (e instanceof EntityNotFoundException) {
        throw (EntityNotFoundException)e;
      } else {
        // This is something unsupported so there is probably some wire-level corruption so throw it as runtime so we can
        //  examine what went wrong, at a higher level.
        throw Throwables.propagate(e);
      }
    }    
  }
}
