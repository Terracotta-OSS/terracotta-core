/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import sun.reflect.FieldAccessor;

import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TransparentAccess;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SuppressWarnings("restriction")
public class FieldUtils {
  public final static String CLASS       = "com/tc/util/FieldUtils";

  public final static String GET_DESC    = "(Ljava/lang/Object;Ljava/lang/reflect/Field;Lsun/reflect/FieldAccessor;)";

  private static ThreadLocal allowAccess = new ThreadLocal();

  private static boolean isTCField(Field field) {
    return field.getName().startsWith(ByteCodeUtil.TC_FIELD_PREFIX);
  }

  public static Object get(Object obj, Field field, FieldAccessor fieldAccessor) {
    if (isTCField(field)) {
      return null;
    } else if (!isStaticAndNonRootField(field)) {
      if (ManagerUtil.isRoot(field) || ManagerUtil.isPhysicallyInstrumented(field.getDeclaringClass())) {
        if ((obj instanceof TransparentAccess) && !isStaticField(field)) {
          return resolveReference((TransparentAccess) obj, field);
        } else {
          return resolveReference(obj, field);
        }
      }
    }
    // XXX: disallow field reads of shared logical objects?
    return fieldAccessor.get(obj);
  }

  private static void throwIllegalArgumentException(String type) {
    StringBuffer sb = new StringBuffer("The argument of type ");
    sb.append(type);
    sb.append(" is illegal.");
    throw new IllegalArgumentException(type);
  }

  public static boolean setBoolean(Object obj, boolean value, Field field) throws IllegalAccessException {
    if (!Boolean.TYPE.equals(field.getType())) {
      throwIllegalArgumentException(field.getType().getName());
    }

    return set(obj, Boolean.valueOf(value), field);
  }

  public static boolean setFloat(Object obj, float value, Field field) throws IllegalAccessException {
    Class<?> fieldType = field.getType();

    if (Float.TYPE.equals(fieldType)) {
      return set(obj, new Float(value), field);
    } else {
      return setDouble(obj, value, field);
    }
  }

  public static boolean setDouble(Object obj, double value, Field field) throws IllegalAccessException {
    if (!Double.TYPE.equals(field.getType())) {
      throwIllegalArgumentException(field.getType().getName());
    }

    return set(obj, new Double(value), field);
  }

  public static boolean setChar(Object obj, char value, Field field) throws IllegalAccessException {
    Class<?> fieldType = field.getType();

    if (Character.TYPE.equals(fieldType)) {
      return set(obj, Character.valueOf(value), field);
    } else {
      return setInt(obj, value, field);
    }
  }

  public static boolean setByte(Object obj, byte value, Field field) throws IllegalAccessException {
    Class<?> fieldType = field.getType();

    if (Byte.TYPE.equals(fieldType)) {
      return set(obj, Byte.valueOf(value), field);
    } else {
      return setShort(obj, value, field);
    }
  }

  public static boolean setShort(Object obj, short value, Field field) throws IllegalAccessException {
    Class<?> fieldType = field.getType();

    if (Short.TYPE.equals(fieldType)) {
      return set(obj, Short.valueOf(value), field);
    } else {
      return setInt(obj, value, field);
    }
  }

  public static boolean setInt(Object obj, int value, Field field) throws IllegalAccessException {
    Class<?> fieldType = field.getType();

    if (Integer.TYPE.equals(fieldType)) {
      return set(obj, Integer.valueOf(value), field);
    } else {
      return setLong(obj, value, field);
    }
  }

  public static boolean setLong(Object obj, long value, Field field) throws IllegalAccessException {
    Class<?> fieldType = field.getType();

    if (Long.TYPE.equals(fieldType)) {
      return set(obj, Long.valueOf(value), field);
    } else {
      return setFloat(obj, value, field);
    }
  }

  /*
   * This method bypasses our check to set values to shared objects thru reflection. This is used from TC code base when
   * this is needed. (like subclass of TreeMap, LinkedHashMap cases)
   */
  public static void tcSet(Object target, Object value, Field field) throws IllegalArgumentException,
      IllegalAccessException {
    allowAccess.set(field);
    try {
      field.set(target, value);
    } finally {
      allowAccess.remove();
    }

  }

  private static boolean accessAllowed(Field field) {
    return field == allowAccess.get();
  }

  public static boolean set(Object obj, Object value, Field field) throws IllegalAccessException {
    if (isTCField(field)) { return true; }

    if (accessAllowed(field)) {
      // returning false allows the orignial uninstrumented code to run.
      return false;
    }

    if (isStaticAndNonRootField(field)) { return false; }

    if (ManagerUtil.isRoot(field)) {
      if ((obj instanceof TransparentAccess) && !isStaticField(field)) {
        setValue((TransparentAccess) obj, field, value);
      } else {
        // This is an exception handling since we allow defining a field of an non-
        // instrumented class to be a root.
        setValue(obj, field, value);
      }
      return true;
    }

    if ((obj instanceof Manageable) && (((Manageable) obj).__tc_managed() != null)) {
      if (ManagerUtil.isLogical(obj)) {
        //
        throw new IllegalAccessException(
                                         "Field modification through reflection for non-physical shared object of type "
                                             + obj.getClass().getName() + " is not supported!");
      }

      if (!TransparentAccess.class.isAssignableFrom(field.getDeclaringClass())) {
        //
        throw new IllegalAccessException(
                                         "Field modification through reflection for fields of non-physically instrumented type "
                                             + obj.getClass().getName() + " is not supported!");
      }

      if (obj instanceof TransparentAccess) {
        // field of physically managed object
        setValue((TransparentAccess) obj, field, value);
        return true;
      }
    }

    return false;
  }

  private static boolean isStaticField(Field field) {
    return Modifier.isStatic(field.getModifiers());
  }

  private static boolean isStaticAndNonRootField(Field field) {
    return isStaticField(field) && !ManagerUtil.isRoot(field);
  }

  private static Object resolveReference(TransparentAccess obj, Field field) {
    // XXX: deal with statics
    return obj.__tc_getmanagedfield(fullFieldName(field));
  }

  private static Object resolveReference(Object obj, Field field) {
    String fieldGetterMethodName = fieldGetterMethod(field.getName());
    try {
      Method m = field.getDeclaringClass().getDeclaredMethod(fieldGetterMethodName, (Class[]) null);
      m.setAccessible(true);
      Object retValue = m.invoke(obj, (Object[]) null);
      return retValue;
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  private static void setValue(Object obj, Field field, Object value) {
    String fieldSetterMethodName = fieldSetterMethod(field.getName());
    Class[] setterArgumentsTypes = new Class[] { field.getType() };
    try {
      Method m = field.getDeclaringClass().getDeclaredMethod(fieldSetterMethodName, setterArgumentsTypes);
      m.setAccessible(true);
      m.invoke(obj, new Object[] { value });
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }

  }

  private static void setValue(TransparentAccess obj, Field field, Object value) {
    // XXX: deal with statics
    obj.__tc_setmanagedfield(fullFieldName(field), value);
  }

  private static String fullFieldName(Field field) {
    return new StringBuffer(field.getDeclaringClass().getName()).append('.').append(field.getName()).toString();
  }

  /**
   * fieldGetterMethod and fieldSetterMethod methods are copied from ByteCodeUtil in order not to put ByteCodeUtil in
   * the boot jar.
   */
  private static String fieldGetterMethod(String fieldName) {
    return ByteCodeUtil.TC_METHOD_PREFIX + "get" + fieldName;
  }

  private static String fieldSetterMethod(String fieldName) {
    return ByteCodeUtil.TC_METHOD_PREFIX + "set" + fieldName;
  }
}
