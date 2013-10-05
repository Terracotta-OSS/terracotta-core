/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

    boolean bool = LiteralValues.isLiteral(clazzName) || config.isLogical(clazzName) || clazz.isArray()
                   || clazz == Object.class;
    portableCache.put(clazz, Boolean.valueOf(bool));
    return bool;
  }


  @Override
  public boolean isPortableInstance(Object obj) {
    if (obj == null) return true;
    return isPortableClass(obj.getClass());
  }

}
