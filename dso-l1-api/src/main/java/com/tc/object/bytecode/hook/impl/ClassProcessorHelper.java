/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.DSOContext;

import java.util.Map;
import java.util.WeakHashMap;

public class ClassProcessorHelper {

  // This map should only hold a weak reference to the loader (key).
  // If we didn't we'd prevent loaders from being GC'd
  private static final Map contextMap = new WeakHashMap();

  /**
   * WARNING: Used by test framework only
   * 
   * @param loader Loader
   * @param context DSOContext
   */
  public static void setContext(ClassLoader loader, DSOContext context) {
    if ((loader == null) || (context == null)) {
      // bad dog
      throw new IllegalArgumentException("Loader and/or context may not be null");
    }

    synchronized (contextMap) {
      contextMap.put(loader, context);
    }
  }

  /**
   * WARNING: used by test framework only
   */
  public static Manager getManager(ClassLoader caller) {
    DSOContext context;
    synchronized (contextMap) {
      context = (DSOContext) contextMap.get(caller);
    }
    if (context == null) { return null; }
    return context.getManager();
  }

}
