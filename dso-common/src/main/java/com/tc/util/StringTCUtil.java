/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.object.bytecode.JavaLangStringTC;

/**
 * This class facilitates easy invoking of java.lang.String adapted methods. The same can be done by Reflection, but we
 * liked this hack :)
 */
public class StringTCUtil {
  /**
   * Check whether the String is interned.
   * 
   * @param string Object
   * @return true if the string is interned
   */
  public static boolean isInterned(Object string) {
    if ((string instanceof JavaLangStringTC)) { return ((JavaLangStringTC) string).__tc_isInterned(); }
    return false;
  }

  /**
   * Invoke String method __tc_intern
   * 
   * @param string Object
   * @return string interned
   */
  public static String intern(Object string) {
    if (string instanceof JavaLangStringTC) {
      return ((JavaLangStringTC) string).__tc_intern();
    } else {
      throw Assert.failure("Expected to call JavaLangStringIntern.__tc_intern() on a String");
    }
  }

  /**
   * Invoke String method __tc_decompress
   * 
   * @param string Object
   */

  public static void decompress(Object string) {
    if (string instanceof JavaLangStringTC) {
      ((JavaLangStringTC) string).__tc_decompress();
    } else {
      throw Assert.failure("Expected to call JavaLangStringIntern.__tc_decompress() on a String");
    }
  }

}
