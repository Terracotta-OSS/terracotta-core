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
