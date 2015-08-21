package com.terracotta.connection.entity;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityMaintenanceRef;
import org.terracotta.entity.EntityClientService;

import com.tc.entity.VoltronEntityMessage;
import com.tc.object.ClientEntityManager;
import com.tc.object.EntityID;
import com.tc.util.Util;
import com.terracotta.connection.entity.MaintenanceModeService;


public class TerracottaMaintenanceModeRef<T extends Entity, C> implements EntityMaintenanceRef<T, C> {
  private final ClientEntityManager entityManager;
  private final MaintenanceModeService maintenanceModeService;
  private final Class<T> type;
  private final long version;
  private final String name;
  private final EntityClientService<T, C> entityClientService;

  public TerracottaMaintenanceModeRef(ClientEntityManager entityManager, MaintenanceModeService maintenanceModeService,
                             Class<T> type, long version, String name, EntityClientService<T, C> entityClientService) {
    this.entityManager = entityManager;
    this.maintenanceModeService = maintenanceModeService;
    this.type = type;
    this.version = version;
    this.name = name;
    this.entityClientService = entityClientService;
    
    // Enter maintenance mode since this instance can only exist while in maintenance mode.
    this.maintenanceModeService.enterMaintenanceMode(type, name);
  }
  
  @Override
  public synchronized boolean doesExist() {
    return this.entityManager.doesEntityExist(getEntityID(), this.version);
  }
  
  @Override
  public synchronized void destroy() {
    Future<Void> future = this.entityManager.destroyEntity(getEntityID(), this.version, Collections.emptySet());
    boolean interrupted = false;
    while (true) {
      try {
        future.get();
        break;
      } catch (InterruptedException e) {
        interrupted = true;
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
    Util.selfInterruptIfNeeded(interrupted);
  }

  @Override
  public synchronized void create(C configuration) {
    boolean doesExist = this.entityManager.doesEntityExist(getEntityID(), this.version);
    if (!doesExist) {
      try {
        this.entityManager.createEntity(getEntityID(), this.version, Collections.singleton(VoltronEntityMessage.Acks.APPLIED), entityClientService.serializeConfiguration(configuration)).get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new IllegalStateException("Already exists");
    }
  }

  @Override
  /**
   * Note that this is to close maintenance mode.
   */
  public synchronized void close() {
    maintenanceModeService.exitMaintenanceMode(type, name);
  }

  private EntityID getEntityID() {
    return new EntityID(type.getName(), name);
  }
}
