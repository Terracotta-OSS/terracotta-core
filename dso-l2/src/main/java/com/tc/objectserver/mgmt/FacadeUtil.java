/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.mgmt;

import com.tc.object.dna.impl.ClassInstance;
import com.tc.object.dna.impl.UTF8ByteDataHolder;

public class FacadeUtil {
  /**
   * If this is returned it means the field has been removed from its parent type, and so no longer exists in the server
   * state instance for that parent type.
   */
  public static final String UNKNOWN_TYPE = "unknown";

  public static String getFieldType(Object value) {
    // XXX: this is kinda wrong actually...we'll end up returning "Integer" for "int" fields and what not.
    if (value != null) {
      return getShortClassName(value.getClass().getName());
    } else {
      return UNKNOWN_TYPE;
    }
  }

  private static String getShortClassName(String className) {
    char chars[] = className.toCharArray();
    int lastDot = 0;
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == '.') {
        lastDot = i + 1;
        continue;
      }
      if (chars[i] == '$') chars[i] = '.';
    }

    return new String(chars, lastDot, chars.length - lastDot);
  }

  public static Object processValue(Object value) {
    if (value instanceof UTF8ByteDataHolder) {
      value = ((UTF8ByteDataHolder) value).asString();
    } else if (value instanceof ClassInstance) {
      value = ((ClassInstance) value).getName().asString();
    }
    return value;
  }

}
