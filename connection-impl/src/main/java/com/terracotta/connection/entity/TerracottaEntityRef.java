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

import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import com.tc.entity.VoltronEntityMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientEntityManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.util.Assert;
import com.tc.util.Util;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;


public class TerracottaEntityRef<T extends Entity, C> implements EntityRef<T, C> {
  private final TCLogger logger = TCLogging.getLogger(TerracottaEntityRef.class);
  private final ClientEntityManager entityManager;
  private final MaintenanceModeService maintenanceModeService;
  private final Class<T> type;
  private final long version;
  private final String name;
  private final EntityClientService<T, C> entityClientService;

  // Each instance fetched by this ref can be individually addressed by the server so it needs a unique ID.
  private final AtomicLong nextClientInstanceID;

  public TerracottaEntityRef(ClientEntityManager entityManager, MaintenanceModeService maintenanceModeService,
                             Class<T> type, long version, String name, EntityClientService<T, C> entityClientService, 
                            AtomicLong clientIds) {
    this.entityManager = entityManager;
    this.maintenanceModeService = maintenanceModeService;
    this.type = type;
    this.version = version;
    this.name = name;
    this.entityClientService = entityClientService;
    this.nextClientInstanceID = clientIds;
  }

  @Override
  public synchronized T fetchEntity() throws EntityNotFoundException, EntityVersionMismatchException {
    maintenanceModeService.readLockEntity(type, name);
    // We need to pass the corresponding unlock into the lookupEntity.  It is responsible for calling the hook
    //  if it fails to look up the entity OR delegating that responsibility to the end-point it found for it to
    //  call when it is closed.
    Runnable closeHook = new Runnable() {
      public void run() {
        maintenanceModeService.readUnlockEntity(type, name);
      }
    };
    
    EntityClientEndpoint endpoint = null;
    try {
      ClientInstanceID clientInstanceID = new ClientInstanceID(this.nextClientInstanceID.getAndIncrement());
      EntityDescriptor entityDescriptor = new EntityDescriptor(getEntityID(), clientInstanceID, this.version);
      endpoint = this.entityManager.fetchEntity(entityDescriptor, closeHook);
    } catch (EntityException e) {
      // In this case, we want to close the endpoint but still throw back the exception.
      closeHook.run();
      // Note that we must externally only present the specific exception types we were expecting.  Thus, we need to check
      // that this is one of those supported types, asserting that there was an unexpected wire inconsistency, otherwise.
      if (e instanceof EntityNotFoundException) {
        throw (EntityNotFoundException)e;
      } else if (e instanceof EntityVersionMismatchException) {
        throw (EntityVersionMismatchException)e;
      } else {
        Assert.failure("Unsupported exception type returned to fetch", e);
      }
    } catch (final Throwable t) {
      closeHook.run();
      Util.printLogAndRethrowError(t, logger);
    }
    
    // Note that a failure to resolve the endpoint would have thrown so this can't be null.
    Assert.assertNotNull(endpoint);
    return entityClientService.create(endpoint);
  }

  @Override
  public String getName() {
    return name;
  }

  private EntityID getEntityID() {
    return new EntityID(type.getName(), name);
  }

  @Override
  public void create(C configuration) throws EntityNotProvidedException, EntityAlreadyExistsException, EntityVersionMismatchException {
    EntityID entityID = getEntityID();
    this.maintenanceModeService.enterMaintenanceMode(this.type, this.name);
    try {
      this.entityManager.createEntity(entityID, this.version, Collections.singleton(VoltronEntityMessage.Acks.APPLIED), entityClientService.serializeConfiguration(configuration)).get();
    } catch (EntityException e) {
      // Note that we must externally only present the specific exception types we were expecting.  Thus, we need to check
      // that this is one of those supported types, asserting that there was an unexpected wire inconsistency, otherwise.
      if (e instanceof EntityNotProvidedException) {
        throw (EntityNotProvidedException)e;
      } else if (e instanceof EntityAlreadyExistsException) {
        throw (EntityAlreadyExistsException)e;
      } else if (e instanceof EntityVersionMismatchException) {
        throw (EntityVersionMismatchException)e;
      } else {
        Assert.failure("Unsupported exception type returned to create", e);
      }
    } catch (InterruptedException e) {
      // We don't expect an interruption here.
      throw new RuntimeException(e);
    } finally {
      this.maintenanceModeService.exitMaintenanceMode(this.type, this.name);
    }
  }

  @Override
  public void destroy() throws EntityNotFoundException {
    EntityID entityID = getEntityID();
    this.maintenanceModeService.enterMaintenanceMode(this.type, this.name);
    try {
      InvokeFuture<byte[]> future = this.entityManager.destroyEntity(entityID, this.version, Collections.<VoltronEntityMessage.Acks>emptySet());
      boolean interrupted = false;
      while (true) {
        try {
          future.get();
          break;
        } catch (EntityException e) {
          // Note that we must externally only present the specific exception types we were expecting.  Thus, we need to check
          // that this is one of those supported types, asserting that there was an unexpected wire inconsistency, otherwise.
          if (e instanceof EntityNotFoundException) {
            throw (EntityNotFoundException)e;
          } else {
            Assert.failure("Unsupported exception type returned to destroy", e);
          }
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
      Util.selfInterruptIfNeeded(interrupted);
    } finally {
      this.maintenanceModeService.exitMaintenanceMode(this.type, this.name);
    }
  }
}
