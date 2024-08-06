/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.tc.objectserver.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.classloader.PermanentEntity;
import com.tc.classloader.PermanentEntityType;
import com.tc.classloader.ServiceLocator;
import com.tc.entity.VoltronEntityMessage;
import com.tc.object.EntityID;

import java.util.ArrayList;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.EntityServerService;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author twu
 */
public class ServerEntityFactory {
  private final ServiceLocator locator;
  private List<VoltronEntityMessage> entityMessages;
  
  public ServerEntityFactory(ServiceLocator loader) {
    this.locator = loader;
  }

  public <T extends EntityServerService<? extends ActiveServerEntity, ? extends PassiveServerEntity>> T getService(String typeName) throws ClassNotFoundException {
    List<Class<? extends EntityServerService>> serviceLoader = locator.getImplementations(EntityServerService.class);
    for (Class<? extends EntityServerService> serverService : serviceLoader) {
      try {
        EntityServerService instance = serverService.newInstance();
        if (instance.handlesEntityType(typeName)) {
          return (T)instance;
        }
      } catch (IllegalAccessException | InstantiationException i) {
        throw new RuntimeException(i);
      }

    }
    throw new ClassNotFoundException(typeName);
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public synchronized List<VoltronEntityMessage> getAnnotatedEntities() {
    if (entityMessages == null) {
      entityMessages = new ArrayList<>();
      List<Class<? extends EntityServerService>> serviceLoader = this.locator.getImplementations(EntityServerService.class);
      for (Class<? extends EntityServerService> serverService : serviceLoader) {
        for (PermanentEntity p : serverService.getAnnotationsByType(PermanentEntity.class)) {
          entityMessages.add(createMessage(p.type(), p.name(), p.version(), TCByteBufferFactory.getInstance(0)));
        }
        for (PermanentEntityType p : serverService.getAnnotationsByType(PermanentEntityType.class)) {
          entityMessages.add(createMessage(p.type().getName(), p.name(), p.version(), TCByteBufferFactory.getInstance(0)));
        }
      }
    }
    return entityMessages;
  }

  public boolean isPermanentEntity(EntityID entity) {
    Predicate<EntityID> sameName = e -> e.getEntityName().equals(entity.getEntityName());
    Predicate<EntityID> sameClass = e -> e.getClassName().equals(entity.getClassName());
    return getAnnotatedEntities().stream()
        .map(p -> p.getEntityDescriptor().getEntityID())
        .anyMatch(sameName.and(sameClass));
  }

  public static VoltronEntityMessage createMessage(String type, String name, int version, TCByteBuffer data) {
    return new CreateSystemEntityMessage(new EntityID(type, name),version, data);
  }
}
