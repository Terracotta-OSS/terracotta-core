/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Silly little class to extract all "static final String" constants (ie. name is ALL CAPS) from a given class
 */
public class Keys {

  public static String[] getKeys(Class clazz) {
    if (clazz == null) { return new String[] {}; }

    List keys = new ArrayList();
    Field[] fields = clazz.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];
      field.setAccessible(true);
      int access = field.getModifiers();
      if ((field.getType().equals(String.class)) && Modifier.isStatic(access) && Modifier.isFinal(access)) {
        String fieldName = field.getName();
        if (fieldName.toUpperCase().equals(fieldName)) {
          try {
            keys.add(field.get(clazz));
          } catch (IllegalAccessException e) {
            e.printStackTrace(); 
          }
        }
      }
    }

    String[] rv = new String[keys.size()];
    return (String[]) keys.toArray(rv);
  }

}
