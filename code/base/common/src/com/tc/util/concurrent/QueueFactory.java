/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.util.runtime.Vm;

import java.lang.reflect.Constructor;

public class QueueFactory {

  public static FastQueue createInstance() {
    FastQueue queue = null;
    if (!Vm.isJDK15Compliant()) {
      queue = new TCBoundedLinkedQueue();
    }else{
      try {
        Class clazz = Class.forName("com.tc.util.concurrent.TCLinkedQueue");
        Constructor constructor = clazz.getConstructor(new Class[0]);
        queue = (FastQueue) constructor.newInstance(new Object[0]);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
    return queue;
  }
}
