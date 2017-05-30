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
package com.terracotta.connection;

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.helper.client.HelperEntity;
import org.terracotta.helper.common.HelperEntityConstants;

import com.tc.object.ClientEntityManager;
import com.terracotta.connection.entity.TerracottaEntityRef;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;


public class TerracottaConnection implements Connection {
  private final ClientEntityManager entityManager;
  private final Runnable shutdown;
  private final ConcurrentMap<Class<? extends Entity>, EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?>> cachedEntityServices = new ConcurrentHashMap<Class<? extends Entity>, EntityClientService<?, ?, ? extends EntityMessage, ? extends EntityResponse, ?>>();
  private final AtomicLong  clientIds = new AtomicLong(1); // initialize to 1 because zero client is a special case for uninitialized
  private final Set<TerracottaEntityRef> entityRefs = Collections.newSetFromMap(new WeakHashMap<TerracottaEntityRef, Boolean>());
  private final HelperEntity helperEntity;

  private boolean isShutdown = false;

  public TerracottaConnection(final ClientEntityManager entityManager, Runnable shutdown) {
    this.entityManager = entityManager;
    this.shutdown = shutdown;
    HelperEntity tmpHelperEntity = null;
    try {
      tmpHelperEntity = getEntityRef(HelperEntity.class, HelperEntityConstants.HELPER_ENTITY_VERSION, HelperEntityConstants.HELPER_ENTITY_NAME).fetchEntity(null);
    } catch (EntityNotProvidedException e) {
      //should not happen
    } catch (EntityNotFoundException e) {
      //should not happen
    } catch (EntityVersionMismatchException e) {
      //should not happen
    }
    this.helperEntity = tmpHelperEntity;
  }

  @Override
  public synchronized <T extends Entity, C, U> EntityRef<T, C, U> getEntityRef(Class<T> cls, long version, String name) throws EntityNotProvidedException {
    checkShutdown();
    @SuppressWarnings("unchecked")
    EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> service = (EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U>)getEntityService(cls);
    if (null == service) {
      // We failed to find a provider for this class.
      throw new EntityNotProvidedException(cls.getName(), name);
    }
    TerracottaEntityRef<T, C, U> terracottaEntityRef = new TerracottaEntityRef<T, C, U>(this.entityManager, cls, version, name, service, clientIds);
    entityRefs.add(terracottaEntityRef);
    return terracottaEntityRef;
  }

  private <T extends Entity, U> EntityClientService<T, ?, ? extends EntityMessage, ? extends EntityResponse, U> getEntityService(Class<T> entityClass) {
    @SuppressWarnings("unchecked")
    EntityClientService<T, ?, ? extends EntityMessage, ? extends EntityResponse, U> service = (EntityClientService<T, ?, ? extends EntityMessage, ? extends EntityResponse, U>) cachedEntityServices.get(entityClass);
    if (service == null) {
      service = EntityClientServiceFactory.creationServiceForType(entityClass, TerracottaConnection.class.getClassLoader());
      if (null != service) {
        @SuppressWarnings("unchecked")
        EntityClientService<T, ?, ? extends EntityMessage, ? extends EntityResponse, U> tmp = (EntityClientService<T, ?, ? extends EntityMessage, ? extends EntityResponse, U>) cachedEntityServices.putIfAbsent(entityClass, service);
        service = tmp == null ? service : tmp;
      }
    }
    return service;
  }

  @Override
  public synchronized void close() {
    checkShutdown();
    helperEntity.close();
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
