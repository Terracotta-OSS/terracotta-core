/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.loaders;

/**
 * An interface to add to existing classloaders to allow it to have a "name" associated with it
 */
public interface NamedClassLoader {

  public static final String CLASS = "com/tc/object/loaders/NamedClassLoader";
  public static final String TYPE  = "L" + CLASS + ";";

  public String __tc_getClassLoaderName();

  public void __tc_setClassLoaderName(String name);

}
