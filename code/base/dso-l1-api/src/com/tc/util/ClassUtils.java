/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;

/**
 * Class utility methods
 */
public class ClassUtils {

  private static final Class METHOD_CLASS = Method.class;
  private static final Class CONSTRUCTOR_CLASS = Constructor.class;
  private static final Class FIELD_CLASS = Field.class;

  /**
   * Convert fully-qualified field name like "mypackage.MyClass.myField" into a specification which 
   * contains the fully-qualified class name and the field name.  
   * @param fieldName Fully-qualified field name 
   * @return Specification of class/field names
   * @throws ParseException If the fieldName is not properly formatted
   */
  public static ClassSpec parseFullyQualifiedFieldName(String fieldName) throws ParseException {
    ClassSpecImpl rv = new ClassSpecImpl();
    rv.parseFullyQualifiedFieldName(fieldName);
    return rv;
  }

  /**
   * Get the dimension of an array
   * @param arrayClass The array class
   * @return Dimension, >= 0
   * @throws NullPointerException If arrayClass is null
   * @throws IllegalArgumentException If arrayClass is not an array class
   */
  public static int arrayDimensions(Class arrayClass) {
    verifyIsArray(arrayClass); // guarantees c is non-null and an array class
    return arrayClass.getName().lastIndexOf("[") + 1;
  }

  /**
   * If c is an array, return the reifiable type of the array element
   * @param c Array class
   * @return Type of an array element
   * @throws NullPointerException If arrayClass is null
   * @throws IllegalArgumentException If arrayClass is not an array class
   */
  public static Class baseComponentType(Class c) {
    verifyIsArray(c);   // guarantees c is non-null and an array class
    while (c.isArray()) {
      c = c.getComponentType();
    }
    return c;
  }

  private static void verifyIsArray(Class arrayClass) {
    if (arrayClass == null) { throw new NullPointerException(); }
    if (!arrayClass.isArray()) { throw new IllegalArgumentException(arrayClass + " is not an array type"); }
  }
  
  /**
   * Determine whether test is a primitive array
   * @param test The object
   * @return True if test is a non-null primitive array
   */
  public static boolean isPrimitiveArray(Object test) {
    if (test == null) { return false; }
    Class c = test.getClass();
    if (!c.isArray()) { return false; }
    return c.getComponentType().isPrimitive();
  }

  /**
   * Determine whether c is an enum (JDK 1.4 friendly)
   * @param c Class  
   * @return True if enum
   */
  public static boolean isEnum(Class c) {
    // a jdk1.4 friendly (but still fast) check for enums
    Class superClass = c.getSuperclass();
    if (superClass == null) return false;
    if (((c.getModifiers() & 0x00004000) != 0) && isSubclassOfEnum(superClass)) { return true; }
    return false;
  }
  
  private static boolean isSubclassOfEnum(Class c) {
    String name = c.getName();
    while (!"java.lang.Enum".equals(name)) {
      c = c.getSuperclass();
      if (c == null) { return false; }
      name = c.getName();
    }
    return true;
  }

  /**
   * Check whether c is a portable java reflection class like Method, Constructor, or Field
   * @param c Class
   * @return True if portable
   */
  public static boolean isPortableReflectionClass(Class c) {
    return METHOD_CLASS == c || CONSTRUCTOR_CLASS == c || FIELD_CLASS == c;
  }

  /**
   * Holder for a class name and field name which together fully identify a field
   * @see ClassUtils#parseFullyQualifiedFieldName(String)
   */
  public interface ClassSpec {
    /**
     * @return Full class name
     */
    public String getFullyQualifiedClassName();

    /**
     * @return Short field name
     */
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
