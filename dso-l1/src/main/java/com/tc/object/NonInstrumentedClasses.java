/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import java.util.HashSet;
import java.util.Set;

public class NonInstrumentedClasses {
  private static final Set nonInstrumentedClass = new HashSet();

  static {
    nonInstrumentedClass.add("java.lang.Object");
    nonInstrumentedClass.add("java.lang.Number");
    nonInstrumentedClass.add("java.util.AbstractList");
    nonInstrumentedClass.add("java.util.AbstractCollection");
    nonInstrumentedClass.add("java.util.AbstractQueue");
    nonInstrumentedClass.add("java.lang.Enum");
  }

  public boolean isInstrumentationNotNeeded(String clazzName) {
    return nonInstrumentedClass.contains(clazzName);
  }
}
