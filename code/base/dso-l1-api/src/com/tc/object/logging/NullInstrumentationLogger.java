/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.logging;

import com.tc.object.config.LockDefinition;

import java.util.Collection;

public class NullInstrumentationLogger implements InstrumentationLogger {
  public boolean getClassInclusion() {
    return false;
  }

  public void setClassInclusion(boolean classInclusion) {
    //
  }

  public boolean getLockInsertion() {
    return false;
  }

  public void setLockInsertion(boolean lockInsertion) {
    //
  }

  public boolean getRootInsertion() {
    return false;
  }

  public void setRootInsertion(boolean rootInsertion) {
    //
  }

  public boolean getDistMethodCallInsertion() {
    return false;
  }

  public void setDistMethodCallInsertion(boolean distMethodClassInsertion) {
    //
  }

  public boolean getTransientRootWarning() {
    return false;
  }

  public void setTransientRootWarning(boolean transientRootWarning) {
    //
  }

  public void classIncluded(String className) {
    //
  }

  public void autolockInserted(String className, String methodName, String methodDesc, LockDefinition lockDef) {
    //
  }

  public void lockInserted(String className, String methodName, String methodDesc, LockDefinition[] locks) {
    //
  }

  public void subclassOfLogicallyManagedClasses(String className, Collection logicalSuperClasses) {
    //
  }

  public void transientRootWarning(String className, String fieldName) {
    //
  }

  public void rootInserted(String className, String fieldName, String desc, boolean isStatic) {
    //
  }

  public void distMethodCallInserted(String className, String methodName, String desc) {
    //
  }
}