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

/**
 * @author twu
 */
public class ServerEntityFactory {
  private final ServiceLocator locator;
  
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
  public List<VoltronEntityMessage> getAnnotatedEntities() {
    List<VoltronEntityMessage> msgs = new ArrayList<>();
    List<Class<? extends EntityServerService>> serviceLoader = this.locator.getImplementations(EntityServerService.class);
    for (Class<? extends EntityServerService> serverService : serviceLoader) {
      for (PermanentEntity p : serverService.getAnnotationsByType(PermanentEntity.class)) {
        msgs.add(createMessage(p.type(), p.name(), p.version(), TCByteBufferFactory.getInstance(false, 0)));
      }
      for (PermanentEntityType p : serverService.getAnnotationsByType(PermanentEntityType.class)) {
        msgs.add(createMessage(p.type().getName(), p.name(), p.version(), TCByteBufferFactory.getInstance(false, 0)));
      }
    }
    return msgs;
  }  

  public static VoltronEntityMessage createMessage(String type, String name, int version, TCByteBuffer data) {
    return new CreateSystemEntityMessage(new EntityID(type, name),version, data);
  }
}
