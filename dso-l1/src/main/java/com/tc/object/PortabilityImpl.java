/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.tc.object.config.DSOClientConfigHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PortabilityImpl implements Portability {

  private final Map<Class, Boolean>           portableCache          = new ConcurrentHashMap();
  private final DSOClientConfigHelper         config;

  public PortabilityImpl(DSOClientConfigHelper config) {
    this.config = config;
  }

  /*
   * This method does not rely on the config but rather on the fact that the class has to be instrumented at this time
   * for the object to be portable. For Logical Objects it still queries the config.
   */
  @Override
  public boolean isPortableClass(final Class clazz) {
    Boolean isPortable = portableCache.get(clazz);
    if (isPortable != null) { return isPortable.booleanValue(); }

    String clazzName = clazz.getName();

    boolean bool = LiteralValues.isLiteral(clazzName) || (config.getSpec(clazzName) != null) || clazz == Object.class;
    portableCache.put(clazz, Boolean.valueOf(bool));
    return bool;
  }


  @Override
  public boolean isPortableInstance(Object obj) {
    if (obj == null) return true;
    return isPortableClass(obj.getClass());
  }

}
