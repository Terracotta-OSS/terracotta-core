/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.text;

public class StringUtils {

  private StringUtils() {
    //
  }

  public static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  public static String trimToNull(String s) {
    if (isBlank(s)) {
      return null;
    }
    
    return s.trim();
  }

  public static boolean isNotBlank(String s) {
    return !isBlank(s);
  }

}
