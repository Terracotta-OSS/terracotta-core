/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.mgmt;

import com.tc.object.dna.impl.ClassInstance;
import com.tc.object.dna.impl.UTF8ByteDataHolder;

public class FacadeUtil {

  public static String getFieldType(Object value) {
    // XXX: this is kinda wrong actually...we'll end up returning "Integer" for "int" fields and what not.
    return getShortClassName(value.getClass().getName());
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
