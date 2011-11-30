/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.util;

import java.net.URL;

/**
 * Utility methods dealing with the context class loader. Fail-over is provided to the default class loader.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public final class ContextClassLoader {

  /**
   * Loads a class starting from the given class loader (can be null, then use default class loader)
   *
   * @param loader
   * @param name   of class to load
   * @return
   * @throws ClassNotFoundException
   */
  public static Class forName(final ClassLoader loader, final String name) throws ClassNotFoundException {
    Class klass = null;
    if (loader != null) {
      klass = Class.forName(name, false, loader);
    } else {
      klass = Class.forName(name, false, ClassLoader.getSystemClassLoader());
    }
    return klass;
  }


  /**
   * Loads a class from the context class loader or, if that fails, from the default class loader.
   *
   * @param name is the name of the class to load.
   * @return a <code>Class</code> object.
   * @throws ClassNotFoundException if the class was not found.
   */
  public static Class forName(final String name) throws ClassNotFoundException {
    Class cls = null;
    try {
      cls = Class.forName(name, false, Thread.currentThread().getContextClassLoader());
    } catch (Exception e) {
      cls = Class.forName(name);
    }
    return cls;
  }

  /**
   * Loads a resource from the context class loader or, if that fails, from the default class loader.
   *
   * @param name is the name of the resource to load.
   * @return a <code>URL</code> object.
   */
  public static URL loadResource(final String name) {
    try {
      return Thread.currentThread().getContextClassLoader().getResource(name);
    } catch (Exception e) {
      return ClassLoader.class.getClassLoader().getResource(name);
    }
  }

//    /**
//     * Loads a resource from the context class loader or, if that fails, from the default class loader, as stream
//     *
//     * @param name is the name of the resource to load.
//     * @return a <code>InputStream</code> object.
//     */
//    public static InputStream getResourceAsStream(final String name) {
//        InputStream stream = null;
//        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
//        if (contextClassLoader != null) {
//            stream = contextClassLoader.getResourceAsStream(name);
//        }
//        if (stream == null) {
//            ClassLoader classLoader = ClassLoader.class.getClassLoader();
//            if (classLoader != null) {
//                stream = classLoader.getResourceAsStream(name);
//            }
//        }
//        return stream;
//    }

  /**
   * Returns the context class loader.
   *
   * @return the context class loader
   */
  public static ClassLoader getLoader() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = ClassLoader.class.getClassLoader();
    }
    return loader;
  }

  /**
   * Returns the given loader or the sytem classloader if loader is null
   *
   * @param loader
   * @return
   */
  public static ClassLoader getLoaderOrSystemLoader(ClassLoader loader) {
    if (loader != null) {
      return loader;
    } else {
      return ClassLoader.getSystemClassLoader();
    }
  }
}