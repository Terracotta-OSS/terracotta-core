/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

/**
 * Defines change applicators to apply for each class.  The change applicator
 * allows a module to replace a class definition if the module needs to swap in an
 * alternate version with some differing functionality in a cluster. 
 * 
 */
public interface ChangeApplicatorSpec {
  
  /**
   * Get the change applicator for a specified class
   * @param clazz The class
   * @return The change applicator if one exists, or null otherwise
   */
  public Class getChangeApplicator(Class clazz);
}
