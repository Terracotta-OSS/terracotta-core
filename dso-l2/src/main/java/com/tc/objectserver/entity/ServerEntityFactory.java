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

import com.tc.classloader.ServiceLocator;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author twu
 */
public class ServerEntityFactory {
  public static <T extends ServerEntityService<? extends ActiveServerEntity, ? extends PassiveServerEntity>> T getService(String typeName) {
    return getService(typeName, ServerEntityFactory.class.getClassLoader());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends ServerEntityService<? extends ActiveServerEntity, ? extends PassiveServerEntity>> T getService(String typeName, ClassLoader classLoader) {
    List<ServerEntityService> serviceLoader = ServiceLocator.getImplementations(ServerEntityService.class, classLoader);
    for (ServerEntityService serverService : serviceLoader) {
      if (serverService.handlesEntityType(typeName)) {
        return (T) serverService;
      }
    }
    throw new IllegalArgumentException("Can't handle entity type " + typeName);
  }
}
