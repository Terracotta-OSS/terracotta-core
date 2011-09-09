/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.lang.reflect.Method;

public abstract class ObjectCloneUtil {

  public static Object clone(Cloneable cloneable) {
    try {
      Method cloneMethod = cloneable.getClass().getDeclaredMethod("clone", new Class[0]);
      if (cloneMethod == null) { throw new AssertionError("Class implements Cloneable but cannot find clone() method: "
                                                          + cloneable.getClass().getName()); }
      cloneMethod.setAccessible(true);
      return cloneMethod.invoke(cloneable);
    } catch (Exception e) {
      throw new RuntimeException("Some problem occured while trying to invoke clone value", e);
    }
  }

  public static Object clone(Object object) {
    if (object instanceof Cloneable) {
      return clone((Cloneable) object);
    } else {
      return object;
    }
  }

}
