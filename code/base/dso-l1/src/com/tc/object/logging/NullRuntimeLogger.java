/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.logging;

import com.tc.object.TCObject;
import com.tc.object.tx.TimerSpec;

public class NullRuntimeLogger implements RuntimeLogger {

  public NullRuntimeLogger() {
    super();
  }

  public boolean getLockDebug() {
    return false;
  }

  public boolean getFieldChangeDebug() {
    return false;
  }

  public void lockAcquired(String lockName, int level, Object instance, TCObject tcobj) {
    return;
  }

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    return;
  }

  public boolean getNewManagedObjectDebug() {
    return false;
  }

  public void newManagedObject(TCObject object) {
    return;
  }

  public boolean getWaitNotifyDebug() {
    return false;
  }

  public void objectNotify(boolean all, Object obj, TCObject tcObject) {
    return;
  }

  public void objectWait(TimerSpec call, Object obj, TCObject tcObject) {
    return;
  }

  public boolean getDistributedMethodDebug() {
    return false;
  }

  public void distributedMethodCall(String receiverName, String methodName, String params) {
    return;
  }

  public void distributedMethodCallError(String receiverClassName, String methodName, String params, Throwable error) {
    return;
  }

  public boolean getArrayChangeDebug() {
    return false;
  }

  public void arrayChanged(TCObject source, int startPos, Object array) {
    return;
  }

  public void literalValueChanged(TCObject source, Object newValue) {
    return;
  }

  public boolean getNonPortableDump() {
    return false;
  }

  public boolean getAutoLockDetails() {
    return false;
  }

  public boolean getCaller() {
    return false;
  }

  public boolean getFullStack() {
    return false;
  }

  public void setArrayChangeDebug(boolean arrayChangeDebug) {
    /**/
  }

  public void setAutoLockDetails(boolean autoLockDetails) {
    /**/
  }

  public void setCaller(boolean caller) {
    /**/
  }

  public void setDistributedMethodDebug(boolean distributedMethodDebug) {
    /**/
  }

  public void setFieldChangeDebug(boolean fieldChangeDebug) {
    /**/
  }

  public void setFullStack(boolean fullStack) {
    /**/
  }

  public void setLockDebug(boolean lockDebug) {
    /**/
  }

  public void setNewManagedObjectDebug(boolean newObjectDebug) {
    /**/
  }

  public void setNonPortableDump(boolean nonPortableDump) {
    /**/
  }

  public void setWaitNotifyDebug(boolean waitNotifyDebug) {
    /**/
  }
}
