/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

public abstract class ObjectCloneUtil {

  public static Object cloneCloneable(Cloneable cloneable) {
    try {
      Method cloneMethod = cloneable.getClass().getDeclaredMethod("clone", new Class[0]);
      cloneMethod.setAccessible(true);
      return cloneMethod.invoke(cloneable);
    } catch (NoSuchMethodException e) {
      throw new AssertionError("Class implements Cloneable but cannot find clone() method: "
                               + cloneable.getClass().getName());
    } catch (Exception e) {
      throw new RuntimeException("Some problem occured while trying to invoke clone value", e);
    }
  }

  public static Object cloneArray(Object array) {
    if (!array.getClass().isArray()) { throw new AssertionError("Object being cloned of type: " + array.getClass()
                                                                + " is not an array."); }
    Object clone = Array.newInstance(array.getClass().getComponentType(), Array.getLength(array));
    System.arraycopy(array, 0, clone, 0, Array.getLength(array));
    return clone;
  }

  public static Object clone(Object object) {
    if (object == null) { return null; }
    if (object.getClass().isArray()) {
      return cloneArray(object);
    } else if (object instanceof Cloneable) {
      return cloneCloneable((Cloneable) object);
    } else {
      return object;
    }
  }

}
