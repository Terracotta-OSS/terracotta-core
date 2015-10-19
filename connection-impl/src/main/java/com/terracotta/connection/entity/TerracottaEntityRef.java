package com.terracotta.connection.entity;

import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.exception.EntityException;

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
  public synchronized T fetchEntity() throws EntityException {
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
      throw e;
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
  public void create(C configuration) throws EntityException {
    EntityID entityID = getEntityID();
    this.maintenanceModeService.enterMaintenanceMode(this.type, this.name);
    try {
      this.entityManager.createEntity(entityID, this.version, Collections.singleton(VoltronEntityMessage.Acks.APPLIED), entityClientService.serializeConfiguration(configuration)).get();
    } catch (InterruptedException e) {
      // We don't expect an interruption here.
      throw new RuntimeException(e);
    } finally {
      this.maintenanceModeService.exitMaintenanceMode(this.type, this.name);
    }
  }

  @Override
  public void destroy() throws EntityException {
    EntityID entityID = getEntityID();
    this.maintenanceModeService.enterMaintenanceMode(this.type, this.name);
    try {
      InvokeFuture<byte[]> future = this.entityManager.destroyEntity(entityID, this.version, Collections.<VoltronEntityMessage.Acks>emptySet());
      boolean interrupted = false;
      while (true) {
        try {
          future.get();
          break;
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
