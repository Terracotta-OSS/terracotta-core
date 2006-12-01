/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import java.lang.reflect.Method;

/**
 * An method invocation event fired by Proxy instances created with MethodMonitorProxy. The event is delivered AFTER the
 * method has executed, but before the caller receives any thrown exceptions
 */
public interface MethodInvocationEvent {

  /**
   * The Method that was invoked
   */
  public Method getMethod();

  /**
   * The arguments to the method invocation NOTE: These may be live references to the method arguments. You are NOT
   * advised to modify the state of any of these parameters
   */
  public Object[] getArguments();

  /**
   * The start time of the method invocation as measured by System.currentTimeMillis();
   */
  public long getExecutionStartTime();

  /**
   * The end time of the method invocation as measured by System.currentTimeMillis();
   */
  public long getExecutionEndTime();

  /**
   * The exception (if any) thrown by this method invocation. The value of this method may be null (indficating the lack
   * of a thrown exception)
   */
  public Throwable getException();

  /**
   * The return value of the method invocation. A return value of null can either mean a true "null" return value, or
   * that an exception is being thrown (ie. you should always be checking getException() before getReturnValue()).
   * Additioanlly, a null value here may be the result of the return type of the method being Void.TYPE <br>
   * <br>
   * NOTE: You really don't want to be mucking with the return value, but nothing is stopping you
   */
  public Object getReturnValue();

  /**
   * @return the object upon which the method was invoked.
   */
  public Object getInvokedObject();
}