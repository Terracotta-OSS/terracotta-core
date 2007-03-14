/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.lang.reflect.Method;
import java.text.ParseException;

public class ClassUtils {

  public static ClassSpec parseFullyQualifiedFieldName(String fieldName) throws ParseException {
    ClassSpecImpl rv = new ClassSpecImpl();
    rv.parseFullyQualifiedFieldName(fieldName);
    return rv;
  }

  public static int arrayDimensions(Class arrayClass) {
    if (arrayClass == null) { throw new NullPointerException(); }
    if (!arrayClass.isArray()) { throw new IllegalArgumentException(arrayClass + " is not an array type"); }
    return arrayClass.getName().lastIndexOf("[") + 1;
  }

  public static Class baseComponetType(Class c) {
    if (c == null) { throw new NullPointerException(); }
    if (!c.isArray()) { throw new IllegalArgumentException(c + " is not an array type"); }

    while (c.isArray()) {
      c = c.getComponentType();
    }

    return c;
  }

  public static boolean isPrimitiveArray(Object test) {
    if (test == null) { return false; }
    Class c = test.getClass();
    if (!c.isArray()) { return false; }
    return c.getComponentType().isPrimitive();
  }

  public static boolean isEnum(Class c) {
    // a jdk1.4 friendly (but still fast) check for enums
    Class superClass = c.getSuperclass();
    if (superClass == null) return false;
    if (((c.getModifiers() & 0x00004000) != 0) && "java.lang.Enum".equals(superClass.getName())) { return true; }
    return false;
  }
  
  public static boolean isPortableReflectionClass(Class c) {
    return (Method.class.getName().equals(c.getName()));
  }

  public interface ClassSpec {
    public String getFullyQualifiedClassName();

    public String getShortFieldName();
  }

  private static class ClassSpecImpl implements ClassSpec {

    private String fullyQualifiedClassName;
    private String shortFieldName;

    private void parseFullyQualifiedFieldName(String fieldName) throws ParseException {
      if (fieldName == null) throwNotFullyQualifiedFieldName(fieldName, 0);
      int lastDot = fieldName.lastIndexOf('.');
      if (lastDot <= 0) throwNotFullyQualifiedFieldName(fieldName, 0);
      if (lastDot + 1 == fieldName.length()) throwNotFullyQualifiedFieldName(fieldName, lastDot);
      fullyQualifiedClassName = fieldName.substring(0, lastDot);
      shortFieldName = fieldName.substring(lastDot + 1);
    }

    private void throwNotFullyQualifiedFieldName(String fieldName, int position) throws ParseException {
      throw new ParseException("Not a fully qualified fieldname: " + fieldName, position);
    }

    public String getFullyQualifiedClassName() {
      return this.fullyQualifiedClassName;
    }

    public String getShortFieldName() {
      return this.shortFieldName;
    }

    public String toString() {
      return "ClassSpec[classname=" + fullyQualifiedClassName + ", shortFieldName=" + shortFieldName + "]";
    }
  }

}
