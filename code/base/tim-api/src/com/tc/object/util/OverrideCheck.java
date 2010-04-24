/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.util;

import com.tc.object.bytecode.ByteCodeUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class OverrideCheck {

  public static void check(Class parent, Class subClass) {
    boolean excludeSuper = parent.isAssignableFrom(subClass);

    Set superMethods = methodsFor(parent, false);
    Set subMethods = methodsFor(subClass, excludeSuper);

    List missing = new ArrayList();

    for (Iterator i = superMethods.iterator(); i.hasNext();) {
      String method = (String) i.next();

      if (!subMethods.contains(method)) {
        // This class should be overriding all methods on the super class
        missing.add(method);
      }
    }

    if (!missing.isEmpty()) { throw new RuntimeException("Missing overrides:\n" + missing); }
  }

  private static Set methodsFor(Class c, boolean excludeSuper) {
    Set set = new HashSet();

    while (c != null && c != Object.class) {
      Method[] methods = c.isInterface() ? c.getMethods() : c.getDeclaredMethods();

      for (Method m : methods) {
        int access = m.getModifiers();

        if (Modifier.isStatic(access) || Modifier.isPrivate(access)) {
          continue;
        }

        if (m.getName().startsWith(ByteCodeUtil.TC_METHOD_PREFIX)) {
          continue;
        }

        StringBuffer sig = new StringBuffer();
        sig.append(m.getName()).append('(');

        Class[] parameterTypes = m.getParameterTypes();
        for (int j = 0; j < parameterTypes.length; j++) {
          sig.append(parameterTypes[j].getName());
          if (j < (parameterTypes.length - 1)) {
            sig.append(',');
          }
        }
        sig.append(')');

        set.add(sig.toString());

      }

      if (excludeSuper) { return set; }

      c = c.getSuperclass();
    }

    return set;
  }
}
