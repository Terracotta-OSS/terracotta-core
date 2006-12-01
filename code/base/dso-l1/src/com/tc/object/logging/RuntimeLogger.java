/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import com.tc.object.TCObject;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.tx.WaitInvocation;

/**
 * Logging interface for various DSO runtime events
 */
public interface RuntimeLogger {

  ///////////////////////////////
  // logging options
  ///////////////////////////////

  boolean lockDebug();

  boolean fieldChangeDebug();
  
  boolean arrayChangeDebug();

  boolean newManagedObjectDebug();

  boolean distributedMethodDebug();

  boolean waitNotifyDebug();

  boolean nonPortableObjectWarning();

  ///////////////////////////////
  // log methods
  ///////////////////////////////

  void lockAcquired(String lockName, int level, Object instance, TCObject tcobj);
  
  void literalValueChanged(TCObject source, Object newValue);

  void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index);
  
  void arrayChanged(TCObject source, int startPos, Object array);

  void nonPortableObjectWarning(Object pojo, NonPortableObjectEvent event);

  void newManagedObject(TCObject object);

  void objectNotify(boolean all, Object obj, TCObject tcObject);

  void objectWait(WaitInvocation call, Object obj, TCObject tcObject);

  void distributedMethodCall(String receiverClassName, String methodName, String params);

  void distributedMethodCallError(String obj, String methodName, String params, Throwable error);

}
