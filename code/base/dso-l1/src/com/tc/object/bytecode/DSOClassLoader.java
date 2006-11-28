/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.bytecode;

/**
 * A placeholder for the old DSOClassLoader. This is to allow old programs that say
 * -Djava.system.class.loader=com.tc.object.bytecode.DSOClassLoader to actually be able to start and print the warning
 */
public class DSOClassLoader extends ClassLoader {

  public DSOClassLoader(ClassLoader parent) {
    super(parent);
    System.out.println("WARNING: The DSOClassLoader should no longer be used as the java system classloader");
  }

}
