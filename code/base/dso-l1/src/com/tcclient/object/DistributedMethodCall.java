/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.object;

/**
 * Representation of a distributed method call
 */
public class DistributedMethodCall {
  private final Object   receiver;
  private final Object[] parameters;
  private final String   methodName;
  private final String   paramDesc;

  public DistributedMethodCall(final Object receiver, final Object[] parameters, final String methodName,
                               final String paramDesc) {
    this.receiver = receiver;
    this.parameters = parameters;
    this.methodName = methodName;
    this.paramDesc = paramDesc;
  }

  public Object getReceiver() {
    return receiver;
  }

  public String getMethodName() {
    return this.methodName;
  }

  public String getParameterDesc() {
    return paramDesc;
  }

  public final Object[] getParameters() {
    Object[] rv = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++)
      rv[i] = parameters[i];
    return rv;
  }

  public String toString() {
    return receiver.getClass().getName() + "." + methodName + paramDesc;
  }
}
