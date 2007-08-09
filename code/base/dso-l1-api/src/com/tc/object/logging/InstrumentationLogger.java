/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import com.tc.object.config.ILockDefinition;

import java.util.Collection;

/**
 * Logging interface for the DSO class loading/adaption system
 */
public interface InstrumentationLogger {

  ///////////////////////////////
  // logging options
  ///////////////////////////////

  boolean classInclusion();

  boolean lockInsertion();

  boolean rootInsertion();

  boolean distMethodCallInsertion();

  boolean transientRootWarning();

  ///////////////////////////////
  // log methods
  ///////////////////////////////

  void classIncluded(String className);

  void autolockInserted(String className, String methodName, String methodDesc, ILockDefinition lockDef);

  void lockInserted(String className, String methodName, String methodDesc, ILockDefinition[] locks);

  void subclassOfLogicallyManagedClasses(String className, Collection logicalSuperClasses);

  void transientRootWarning(String className, String fieldName);

  void rootInserted(String className, String fieldName, String desc, boolean isStatic);

  void distMethodCallInserted(String className, String methodName, String desc);
}
