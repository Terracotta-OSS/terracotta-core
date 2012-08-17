/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

public class LoadClassUtil {

  /**
   * The purpose of this method is to call loadClass on the relevant classloader but deal with possibility of class
   * names like "[Ljava.lang.String;" or "[[I" which are what you get from String[].class.getName() and
   * int[][].class.getName(). Normally Class.forName() just takes care of this for you but that method unfortunately
   * caches results in an undesired way
   */
  public static Class<?> loadClass(String name, ClassLoader loader) throws ClassNotFoundException {
    if (loader == null) { return Class.forName(name, false, null); }

    if (name.charAt(0) != '[') { return loader.loadClass(name); }

    int dimensions = 1;
    while (name.charAt(dimensions) == '[') {
      dimensions++;
    }

    String componentType = name.substring(dimensions);

    if (componentType.charAt(0) == 'L') {
      Class componentClass = loader.loadClass(componentType.substring(1, componentType.length() - 1));
      return Class.forName(name, false, componentClass.getClassLoader());
    } else {
      return Class.forName(name, false, null);
    }
  }
}
