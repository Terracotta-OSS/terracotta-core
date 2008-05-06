/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

/**
 * An interface to add to existing classloaders to allow it to have a "name" associated with it
 */
public interface NamedClassLoader {

  /**
   * Get classloader name
   * @return name
   */
  public String __tc_getClassLoaderName();

  /**
   * Set classloader name
   * @param name Name
   */
  public void __tc_setClassLoaderName(String name);

}
