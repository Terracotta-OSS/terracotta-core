/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.simulate.type;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SimulatedTypeFactory {

  private static final Map SIM_TYPES = Collections.synchronizedMap(new HashMap());
  static {
    SIM_TYPES.put(String.class.getName(), SimulatedPrimitiveType.class);
    SIM_TYPES.put(Integer.class.getName(), SimulatedPrimitiveType.class);
    SIM_TYPES.put(Long.class.getName(), SimulatedPrimitiveType.class);
    SIM_TYPES.put(Collection.class.getName(), SimulatedCollection.class);
    SIM_TYPES.put(Map.class.getName(), SimulatedMap.class);
  }

  private SimulatedTypeFactory() {
    // cannot instantiate
  }

  public static SimulatedType create(Object obj) {
    Constructor[] constructor = resolveSimType(obj).getDeclaredConstructors();
    try {
      return (SimulatedType) constructor[0].newInstance(new Object[] { obj });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Class resolveSimType(Object obj) {
    Class simType = (Class) SIM_TYPES.get(obj.getClass().getName());
    if (simType != null) return simType;

    simType = resolveInterface(obj.getClass());
    if (simType != null) return simType;

    throw new RuntimeException("Type not supported: " + obj.getClass().getName());
  }

  private static Class resolveInterface(Class clazz) {
    Class simType = null;
    Class[] interfaces = clazz.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      simType = (Class) SIM_TYPES.get(interfaces[i].getName());
      if (simType != null) return simType;
      simType = resolveInterface(interfaces[i]);
      if (simType != null) return simType;
    }
    return null;
  }
}
