/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

package com.terracotta.connection;

import com.tc.util.ManagedServiceLoader;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;



public class EntityClientServiceFactory {
  private final ManagedServiceLoader loader;
  private final Map<ClassLoader, Collection<Class<? extends EntityClientService>>> cachedEntities = new WeakHashMap<>();

  public EntityClientServiceFactory() {
    loader = new ManagedServiceLoader();
  }
  
  public <T extends Entity, C, U> EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> creationServiceForType(Class<T> cls) {
    EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> foundService = null;
    Collection<Class<? extends EntityClientService>> implementations = getEntityServices(cls.getClassLoader());
    for (Class<? extends EntityClientService> ctype : implementations) {
      EntityClientService instance = instantiate(ctype);
      if (instance.handlesEntityType(cls)) {
        foundService = instance;
        break;
      }
    }
    return foundService;
  }
  
  private synchronized Collection<Class<? extends EntityClientService>> getEntityServices(ClassLoader cl) {
    Collection<Class<? extends EntityClientService>> list = cachedEntities.get(cl);
    if (list == null) {
      list = loader.getImplementationsTypes(EntityClientService.class, cl);
      cachedEntities.put(cl, list);
    }
    return list;
  }
  
  private static EntityClientService instantiate(Class<? extends EntityClientService> entry) {
    try {
      return entry.newInstance();
    } catch (IllegalAccessException a) {
      
    } catch (InstantiationException i) {
      
    }
    return null;
  }
}
