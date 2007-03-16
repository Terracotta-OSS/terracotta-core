/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.logging;

import com.tc.object.TCObject;
import com.tc.object.tx.WaitInvocation;

public class NullRuntimeLogger implements RuntimeLogger {

  public NullRuntimeLogger() {
    super();
  }

  public boolean lockDebug() {
    return false;
  }

  public boolean fieldChangeDebug() {
    return false;
  }

  public void lockAcquired(String lockName, int level, Object instance, TCObject tcobj) {
    return;
  }

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    return;
  }

  public boolean newManagedObjectDebug() {
    return false;
  }

  public void newManagedObject(TCObject object) {
    return;
  }

  public boolean waitNotifyDebug() {
    return false;
  }

  public void objectNotify(boolean all, Object obj, TCObject tcObject) {
    return;
  }

  public void objectWait(WaitInvocation call, Object obj, TCObject tcObject) {
    return;
  }

  public boolean distributedMethodDebug() {
    return false;
  }

  public void distributedMethodCall(String receiverName, String methodName, String params) {
    return;
  }

  public void distributedMethodCallError(String receiverClassName, String methodName, String params, Throwable error) {
    return;
  }

  public boolean arrayChangeDebug() {
    return false;
  }

  public void arrayChanged(TCObject source, int startPos, Object array) {
    return;
  }

  public void literalValueChanged(TCObject source, Object newValue) {
    return;
  }

  public boolean nonPortableDump() {
    return false;
  }

}
