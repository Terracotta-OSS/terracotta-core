/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

/**
 * Manage loading relationship between named classloaders and classes
 */
public interface ClassProvider {

  /**
   * Given a class name, load the class
   * 
   * @param className Class name
   * @return Class
   * @throws ClassNotFoundException If class not found through loader
   */
  Class getClassFor(String className) throws ClassNotFoundException;

}
