/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import java.lang.reflect.Method;

/**
 * A simple implementation of the MethodInvocationEvent interface (heck, I have to write something here)
 */
class MethodInvocationEventImpl implements MethodInvocationEvent {  
  private final long executionStartTime;
  private final long executionEndTime;
  private final Method method;
  private final Object[] args;
  private final Throwable exception;
  private final Object returnValue;  
  private final Object invokedObject;
  
  public MethodInvocationEventImpl(long executionStart, long executionEnd, Object invokedObj, Method method, Object[] args, Throwable exception, Object returnValue) {
    this.executionStartTime = executionStart;
    this.executionEndTime = executionEnd;
    this.method  = method;
    this.args = args;
    this.exception = exception;
    this.returnValue = returnValue;    
    this.invokedObject = invokedObj;
  }

  public Method getMethod() {
    return method;
  }

  public Object[] getArguments() {
    return args;
  }

  public long getExecutionStartTime() {
    return executionStartTime;
  }

  public long getExecutionEndTime() {
    return executionEndTime;
  }

  public Throwable getException() {
    return exception;
  }

  public Object getReturnValue() {
    return returnValue;
  }

  public Object getInvokedObject() {
    return invokedObject;
  }
}