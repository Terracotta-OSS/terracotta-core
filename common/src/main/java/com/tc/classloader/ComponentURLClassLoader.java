/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 */
public class ComponentURLClassLoader extends URLClassLoader {

  public ComponentURLClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }
  
  public ComponentURLClassLoader(URL urls, ClassLoader parent) {
    super(new URL[] {urls} , parent);
  }
  
  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> clazz = findLoadedClass(name);
    if (clazz == null) {
      try {
        clazz = findClass(name);
        if (clazz.getAnnotation(CommonComponent.class) != null) {
          throw new ClassNotFoundException("common api");
        }
      } catch (ClassNotFoundException notfound) {
        clazz = getParent().loadClass(name);
      }
    }
    if (clazz != null && resolve) {
      this.resolveClass(clazz);
    }
    return clazz;
  }
}
