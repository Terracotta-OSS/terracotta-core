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
package com.terracotta.connection;

import com.tc.classloader.ServiceLocator;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientService;

import java.util.List;

/**
 * @author twu
 */
public class EntityClientServiceFactory {
  public static <T extends Entity, C> EntityClientService<T, C> creationServiceForType(Class<T> cls) {
    return creationServiceForType(cls, EntityClientServiceFactory.class.getClassLoader());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends Entity, C> EntityClientService<T, C> creationServiceForType(Class<T> cls, ClassLoader classLoader) {
    List<EntityClientService> implementations = ServiceLocator.getImplementations(EntityClientService.class,  classLoader);
    for (EntityClientService<T, C> entityClientService : implementations) {
      if (entityClientService.handlesEntityType(cls)) {
        return entityClientService;
      }
    }
    throw new IllegalArgumentException("Can't handle entity type " + cls.getName());
  }
}
