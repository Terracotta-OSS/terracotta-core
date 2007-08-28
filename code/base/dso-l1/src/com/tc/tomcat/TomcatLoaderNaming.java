/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.tomcat;

import java.lang.reflect.Method;

public class TomcatLoaderNaming {

  public static String getFullyQualifiedName(Object container) {
    try {
      Class c = container.getClass().getClassLoader().loadClass("org.apache.catalina.Container");
      Method getName = c.getMethod("getName", new Class[] {});
      Method getParent = c.getMethod("getParent", new Class[] {});

      StringBuffer rv = new StringBuffer();

      while (container != null) {
        rv.insert(0, ":" + getName(getName, container));
        container = getParent(getParent, container);
      }

      if (rv.length() > 0) {
        rv.delete(0, 1);
      }

      return rv.toString();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String getName(Method m, Object o) throws Exception {
    return (String) m.invoke(o, new Object[] {});
  }

  private static Object getParent(Method m, Object o) throws Exception {
    return m.invoke(o, new Object[] {});
  }

}
