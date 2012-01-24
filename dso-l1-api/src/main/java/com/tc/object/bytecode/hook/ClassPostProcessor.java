/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook;

/**
 * Modify class after being loaded
 */
public interface ClassPostProcessor {

  /**
   * Post-process the class
   * @param clazz The class
   * @param caller The classloader loading the class
   */
  public void postProcess(Class clazz, ClassLoader caller);

}
