/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Methods to convert Class to Java type names. Handles array types and the constructor "return" type.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:vta@medios.fi">Tibor Varga </a>
 */
public class TypeConverter {
  /**
   * Converts an array of Classes to their Java language declaration equivalents.
   *
   * @param types is the array of <code>Class</code> objects.
   * @return an array of Strings representing the given types. For <code>null</code> types, this method returns
   *         "void"s.
   */
  public static String[] convertTypeToJava(final Class[] types) {
    String[] parameterTypeNames = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      parameterTypeNames[i] = convertTypeToJava(types[i]);
    }
    return parameterTypeNames;
  }

  /**
   * Converts a Class to its Java language declaration equivalent.
   *
   * @param type is the <code>Class</code> object.
   * @return a Strings representing the given types. For <code>null</code> type, this method returns "void".
   */
  public static String convertTypeToJava(final Class type) {
    String rv = null;

    // constructor return type can be null
    if (type != null) {
      StringBuffer dim = new StringBuffer();
      Class componentType = type.getComponentType();
      for (Class nestedType = type; nestedType.isArray(); nestedType = nestedType.getComponentType()) {
        dim.append("[]");
      }

      // Found a component type => we had an array
      if (dim.length() > 0) {
        rv = componentType.getName() + dim;
      } else {
        rv = type.getName();
      }
    } else {
      rv = "void";
    }
    return rv;
  }
}