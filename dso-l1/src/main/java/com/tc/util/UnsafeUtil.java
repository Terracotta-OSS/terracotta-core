/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import sun.misc.Unsafe;

import com.tc.exception.TCRuntimeException;
import com.tc.object.TCObjectExternal;
import com.tc.object.bytecode.ManagerUtil;

import java.lang.reflect.Field;

/**
 * A wrapper for unsafe usage in class like Atomic Variables, ReentrantLock, etc.
 */
@SuppressWarnings("restriction")
public class UnsafeUtil {
  public final static String  CLASS_SLASH       = "com/tc/util/UnsafeUtil";
  public final static String  CLASS_DESCRIPTION = "Lcom/tc/util/UnsafeUtil;";
  private final static Unsafe unsafe            = findUnsafe();

  private UnsafeUtil() {
    // Disallow any object to be instantiated.
  }

  public static void updateDSOSharedField(Object obj, long fieldOffset, Object update) {
    TCObjectExternal tcObject = ManagerUtil.lookupExistingOrNull(obj);
    if (tcObject == null) { throw new NullPointerException("Object is not a DSO shared object."); }
    tcObject.objectFieldChangedByOffset(obj.getClass().getName(), fieldOffset, update, -1);
  }

  public static void setField(Object obj, Field field, Object value) {
    long offset = unsafe.objectFieldOffset(field);
    unsafe.putObject(obj, offset, value);
  }

  public static void monitorEnter(Object obj) {
    unsafe.monitorEnter(obj);
  }

  public static void monitorExit(Object obj) {
    unsafe.monitorExit(obj);
  }

  public static Unsafe getUnsafe() {
    return unsafe;
  }

  private static Unsafe findUnsafe() {
    Class uc = Unsafe.class;
    Field[] fields = uc.getDeclaredFields();
    for (Field field : fields) {
      if (field.getName().equals("theUnsafe")) {
        field.setAccessible(true);
        try {
          return (Unsafe) field.get(uc);
        } catch (IllegalArgumentException e) {
          throw new TCRuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
    return null;
  }
}
