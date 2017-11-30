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

import com.tc.util.ManagedServiceLoader;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;



public class EntityClientServiceFactory {
  private final ManagedServiceLoader loader;
  private final Map<ClassLoader, List<Class<? extends EntityClientService>>> cachedEntities = new WeakHashMap<ClassLoader, List<Class<? extends EntityClientService>>>();

  public EntityClientServiceFactory() {
    loader = new ManagedServiceLoader();
  }
  
  public <T extends Entity, C, U> EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> creationServiceForType(Class<T> cls) {
    EntityClientService<T, C, ? extends EntityMessage, ? extends EntityResponse, U> foundService = null;
    List<Class<? extends EntityClientService>> implementations = getEntityServices(cls.getClassLoader());
    for (Class<? extends EntityClientService> ctype : implementations) {
      EntityClientService instance = instantiate(ctype);
      if (instance.handlesEntityType(cls)) {
        foundService = instance;
        break;
      }
    }
    return foundService;
  }
  
  private synchronized List<Class<? extends EntityClientService>> getEntityServices(ClassLoader cl) {
    List<Class<? extends EntityClientService>> list = cachedEntities.get(cl);
    if (list == null) {
      list = loader.getImplementations(EntityClientService.class, cl);
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
