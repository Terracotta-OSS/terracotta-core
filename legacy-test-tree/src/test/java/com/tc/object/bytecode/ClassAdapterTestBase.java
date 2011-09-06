/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.BaseDSOTestCase;

import java.lang.reflect.Method;

public class ClassAdapterTestBase extends BaseDSOTestCase {
  protected void invokeMethod(Class c, Object instance, String name, Class[] paramTypes, Object[] paramValues)
      throws Exception {
    invokeMethod(c, instance, name, paramTypes, paramValues, true);
  }

  protected Object invokeMethod(Class c, Object instance, String name, Class[] paramTypes, Object[] paramValues,
                              boolean failOnException) throws Exception {
    try {
      Method putMethod = c.getDeclaredMethod(removeDescriptionIfNecessary(name), paramTypes);
      putMethod.setAccessible(true);

      return putMethod.invoke(instance, paramValues);
    } catch (Exception e) {     
      if (failOnException) {
        e.printStackTrace();
        fail("Caught exception: " + e);
      }
      throw e;
    }
  }

  private String removeDescriptionIfNecessary(String methodName) {
    int index = methodName.indexOf('(');
    if (index < 0) {
      return methodName;
    } else {
      return methodName.substring(0, index);
    }
  }
}