/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import java.util.ArrayList;
import java.util.List;

public class NonInstrumentedClasses {
  private static final List nonInstrumentedClass = new ArrayList();

  static {
    nonInstrumentedClass.add("java.lang.Object");
    nonInstrumentedClass.add("java.lang.Number");
    nonInstrumentedClass.add("java.util.AbstractList");
    nonInstrumentedClass.add("java.util.AbstractCollection");
    nonInstrumentedClass.add("java.util.AbstractQueue");
    nonInstrumentedClass.add("java.util.Dictionary");
    nonInstrumentedClass.add("java.lang.Enum");
    nonInstrumentedClass.add("java.lang.reflect.AccessibleObject");
    nonInstrumentedClass.add("java.util.concurrent.atomic.AtomicInteger");
    nonInstrumentedClass.add("java.util.concurrent.atomic.AtomicLong");
  }

  public boolean isInstrumentationNotNeeded(String clazzName) {
    return nonInstrumentedClass.contains(clazzName);
  }
}
