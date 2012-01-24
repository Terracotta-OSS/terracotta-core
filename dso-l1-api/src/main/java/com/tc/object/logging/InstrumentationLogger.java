/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import com.tc.object.config.LockDefinition;

import java.util.Collection;

/**
 * Logging interface for the DSO class loading/adaption system
 */
public interface InstrumentationLogger {

  ///////////////////////////////
  // logging options
  ///////////////////////////////

  /**
   * Determine whether to log when a class is included for instrumentation (checked 
   * before calls to {@link #classIncluded(String)}).
   * @return True if should log
   */
  boolean getClassInclusion();
  void setClassInclusion(boolean classInclusion);
  
  /**
   * Determine whether to log when a lock is inserted (checked before calls to 
   * {@link #autolockInserted(String, String, String, LockDefinition)} or 
   * {@link #lockInserted(String, String, String, LockDefinition[])}).
   * @return True if should log
   */
  boolean getLockInsertion();
  void setLockInsertion(boolean lockInsertion);
  
  /**
   * Determine whether to log when a root is inserted (checked before calls to
   * {@link #rootInserted(String, String, String, boolean)}).
   * @return True if should log
   */
  boolean getRootInsertion();
  void setRootInsertion(boolean rootInsertion);
  
  /**
   * Determine whether to log when a DMI call is inserted (checked before calls 
   * to {@link #distMethodCallInserted(String, String, String)}).
   * @return True if should log
   */
  boolean getDistMethodCallInsertion();
  void setDistMethodCallInsertion(boolean distMethodClassInsertion);
  
  /**
   * Determine whether to log transient root warnings (checked before calls to
   * {@link #transientRootWarning(String, String)).
   * @return True if should log
   */
  boolean getTransientRootWarning();
  void setTransientRootWarning(boolean transientRootWarning);
  
  ///////////////////////////////
  // log methods
  ///////////////////////////////

  /**
   * Log class that is being instrumented
   * @param className Class name
   */
  void classIncluded(String className);

  /**
   * Log that auto lock was inserted
   * @param className The class name
   * @param methodName The method name
   * @param methodDesc Method descriptor
   * @param lockDef The lock definition
   */
  void autolockInserted(String className, String methodName, String methodDesc, LockDefinition lockDef);

  /**
   * Log that lock was inserted
   * @param className The class name
   * @param methodName The method name
   * @param methodDesc Method descriptor
   * @param locks The lock definitions
   */
  void lockInserted(String className, String methodName, String methodDesc, LockDefinition[] locks);

  /**
   * Log that a subclass of a logically managed class cannot be instrumented
   * @param className The class
   * @param logicalSuperClasses All logical super classes that prevent className from being instrumented
   */
  void subclassOfLogicallyManagedClasses(String className, Collection logicalSuperClasses);

  /**
   * Log that the transient property is being ignored for a root
   * @param className Class name
   * @param fieldName Transient field name
   */
  void transientRootWarning(String className, String fieldName);

  /**
   * Log that a root was inserted
   * @param className The class name
   * @param fieldName The root field
   * @param desc Method descriptor
   * @param isStatic True if static root
   */
  void rootInserted(String className, String fieldName, String desc, boolean isStatic);

  /**
   * Log that a DMI call was inserted.  
   * @param className The class name
   * @param methodName The method name
   * @param desc The method descriptor
   */
  void distMethodCallInserted(String className, String methodName, String desc);
}
