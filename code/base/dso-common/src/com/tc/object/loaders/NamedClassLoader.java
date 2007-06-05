/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

/**
 * An interface to add to existing classloaders to allow it to have a "name" associated with it
 */
public interface NamedClassLoader {

  public String __tc_getClassLoaderName();

  public void __tc_setClassLoaderName(String name);

}
