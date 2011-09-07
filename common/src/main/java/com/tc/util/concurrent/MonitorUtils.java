/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

@SuppressWarnings("restriction")
public class MonitorUtils {

  private static final Unsafe unsafe;

  static {
    try {
      Class unsafeClass = Class.forName("sun.misc.Unsafe");
      Field getter = unsafeClass.getDeclaredField("theUnsafe");
      getter.setAccessible(true);
      unsafe = (Unsafe) getter.get(null);
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException("Unable to access sun.misc.Unsafe");
    }
  }

  private MonitorUtils() {
    // utility class -- not for instantiation
  }

  public static void monitorEnter(Object object) {
    unsafe.monitorEnter(object);
  }

  public static void monitorExit(Object object) {
    unsafe.monitorExit(object);
  }

  public static void monitorEnter(Object object, int count) {
    for (int i = 0; i < count; i++) {
      unsafe.monitorEnter(object);
    }
  }

  /**
   * Completely release the monitor on the given object (calling thread needs to own the monitor obviously)
   * 
   * @return the number of monitorExit calls performed
   */
  public static int releaseMonitor(Object object) {
    if (object == null) { throw new NullPointerException("object is null"); }
    if (!Thread.holdsLock(object)) { throw new IllegalMonitorStateException("not monitor owner"); }

    // This has the side effect of inflating the monitor (see VM source). It may not be necessary on all platforms (and
    // can be optimized as such if necessary).
    unsafe.monitorEnter(object);
    unsafe.monitorExit(object);

    int count = 0;
    while (Thread.holdsLock(object)) {
      unsafe.monitorExit(object);
      count++;
    }

    return count++;
  }
}
