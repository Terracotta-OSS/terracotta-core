/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
