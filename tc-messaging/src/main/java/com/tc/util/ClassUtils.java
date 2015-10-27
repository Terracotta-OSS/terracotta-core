/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;

/**
 * Class utility methods
 */
public class ClassUtils {
  
  /**
   * Get the dimension of an array
   * @param arrayClass The array class
   * @return Dimension, >= 0
   * @throws NullPointerException If arrayClass is null
   * @throws IllegalArgumentException If arrayClass is not an array class
   */
  public static int arrayDimensions(Class<?> arrayClass) {
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
  public static Class<?> baseComponentType(Class<?> c) {
    verifyIsArray(c);   // guarantees c is non-null and an array class
    while (c.isArray()) {
      c = c.getComponentType();
    }
    return c;
  }

  private static void verifyIsArray(Class<?> arrayClass) {
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
    Class<?> c = test.getClass();
    if (!c.isArray()) { return false; }
    return c.getComponentType().isPrimitive();
  }

  /**
   * Determine whether the class is an enum as far as DSO is concerned
   * @param c Class
   * @return True if enum
   */
  public static boolean isDsoEnum(Class<?> c) {
    // we don't just return c.isEnum() since that is false for specialized enum types

    while(c.getSuperclass() != null) {
      if (c.isEnum()) return true;
      c = c.getSuperclass();
    }
    return false;
  }

}
