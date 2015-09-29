package com.terracotta.connection;


import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityMaintenanceRef;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientService;

import com.tc.object.ClientEntityManager;
import com.tc.object.locks.ClientLockManager;
import com.terracotta.connection.entity.MaintenanceModeService;
import com.terracotta.connection.entity.TerracottaEntityRef;
import com.terracotta.connection.entity.TerracottaMaintenanceModeRef;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;


public class TerracottaConnection implements Connection {
  private final ClientEntityManager entityManager;
  private final MaintenanceModeService maintenanceModeService;
  private final Runnable shutdown;
  private final ConcurrentMap<Class<? extends Entity>, EntityClientService> cachedEntityServices = new ConcurrentHashMap<>();
  private final AtomicLong  clientIds = new AtomicLong();

  private boolean isShutdown = false;

  public TerracottaConnection(ClientEntityManager entityManager, ClientLockManager clientLockManager, Runnable shutdown) throws IOException {
    this.entityManager = entityManager;
    this.maintenanceModeService = new MaintenanceModeService(clientLockManager);
    this.shutdown = shutdown;
  }

  @Override
  public synchronized <T extends Entity> EntityRef<T> getEntityRef(Class<T> cls, long version, String name) {
    checkShutdown();
    return new TerracottaEntityRef<>(this.entityManager, this.maintenanceModeService, cls, version, name, getEntityService(cls), clientIds);
  }

  @Override
  public <T extends Entity, C> EntityMaintenanceRef<T, C> acquireMaintenanceModeRef(Class<T> cls, long version, String name) {
    checkShutdown();
    return new TerracottaMaintenanceModeRef<>(this.entityManager, maintenanceModeService, cls, version, name, getEntityService(cls));
  }

  private <T extends Entity, C> EntityClientService<T, C> getEntityService(Class<T> entityClass) {
    EntityClientService<T, C> service = cachedEntityServices.get(entityClass);
    if (service == null) {
      service = EntityClientServiceFactory.creationServiceForType(entityClass, TerracottaConnection.class.getClassLoader());
      EntityClientService tmp = cachedEntityServices.putIfAbsent(entityClass, service);
      service = tmp == null ? service : tmp;
    }
    return service;
  }

  @Override
  public synchronized void close() {
    checkShutdown();
    shutdown.run();
    isShutdown = true;
  }

  private void checkShutdown() {
    if (isShutdown) {
      throw new IllegalStateException("Already shut down");
    }
  }

  private static class EntityKey<T extends Entity> {
    private final Class<T> cls;
    private final String name;

    private EntityKey(Class<T> cls, String name) {
      this.cls = cls;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final EntityKey<?> entityKey = (EntityKey<?>) o;

      if (!cls.equals(entityKey.cls)) return false;
      if (!name.equals(entityKey.name)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = cls.hashCode();
      result = 31 * result + name.hashCode();
      return result;
    }
  }
}
