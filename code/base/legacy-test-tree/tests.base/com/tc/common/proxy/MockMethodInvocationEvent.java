/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import java.lang.reflect.Method;

/**
 * Mock version of a method invocation event object.
 */
public class MockMethodInvocationEvent implements MethodInvocationEvent {

  private Method    method;
  private Object[]  arguments;
  private long      executionStartTime;
  private long      executionEndTime;
  private Throwable exception;
  private Object    returnValue;
  private Object    invokedObject;

  public Method getMethod() {
    return method;
  }

  public Object[] getArguments() {
    return arguments;
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

  public void setArguments(Object[] arguments) {
    this.arguments = arguments;
  }

  public void setException(Throwable exception) {
    this.exception = exception;
  }

  public void setExecutionEndTime(long executionEndTime) {
    this.executionEndTime = executionEndTime;
  }

  public void setExecutionStartTime(long executionStartTime) {
    this.executionStartTime = executionStartTime;
  }

  public void setInvokedObject(Object invokedObject) {
    this.invokedObject = invokedObject;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  public void setReturnValue(Object returnValue) {
    this.returnValue = returnValue;
  }
}