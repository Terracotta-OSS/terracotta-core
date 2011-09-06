/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import sun.reflect.ReflectionFactory;

import com.tc.exception.TCRuntimeException;

import java.lang.reflect.Constructor;

/**
 * A wrapper for unsafe usage in class like Atomic Variables, ReentrantLock, etc.
 */
public class ReflectionUtil {
  private static final Constructor       refConstructor;
  private static final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();

  static {
    try {
      refConstructor = Object.class.getDeclaredConstructor(new Class[0]);
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    }
  }

  private ReflectionUtil() {
    // Disallow any object to be instantiated.
  }

  public static Constructor newConstructor(Class clazz, Class logicalSuperClass) {
    Constructor useConstructor = refConstructor;
    if (logicalSuperClass != null) {
      try {
        useConstructor = logicalSuperClass.getDeclaredConstructor(new Class[0]);
      } catch (SecurityException e) {
        throw new TCRuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new TCRuntimeException(e);
      }
    }
    return rf.newConstructorForSerialization(clazz, useConstructor);
  }

}
