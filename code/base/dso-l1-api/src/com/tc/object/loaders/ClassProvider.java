/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

/**
 * Manage loading relationship between named classloaders and classes
 */
public interface ClassProvider {

  /**
   * Given a class name and a classloader name, load the class
   * @param className Class name
   * @param loaderDesc Classloader name
   * @return Class
   * @throws ClassNotFoundException If class not found through loader
   */
  Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException;

  /**
   * Get classloader name that loads class
   * @param clazz Class
   * @return Classloader name
   */
  String getLoaderDescriptionFor(Class clazz);

  /**
   * Get classloader by name
   * @param loaderDesc Classloader name
   * @return Classloader
   */
  ClassLoader getClassLoader(String loaderDesc);

  /**
   * Get name for classloader
   * @param loader Loader
   * @return Name
   */
  String getLoaderDescriptionFor(ClassLoader loader);
  
  /**
   * Register named loader
   * @param loader Loader
   */
  void registerNamedLoader(NamedClassLoader loader);
}
