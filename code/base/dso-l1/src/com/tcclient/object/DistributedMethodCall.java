/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.object;

/**
 * Representation of a distributed method call
 */
public class DistributedMethodCall {
  private final String    receiverClassName;
  private final long      receiverID;
  private final String    methodName;
  private final String    paramDesc;
  private final String    receiverClassLoaderDesc;
  private final boolean[] isRef;
  private final Object[]  parameters;

  public DistributedMethodCall(long receiverID, String receiverClassName, String receiverClassLoaderDesc,
                               String methodDesc, Object[] parameters, boolean[] isRef) {
    if (parameters.length != isRef.length) { throw new AssertionError("Different sized arrays: params.length="
                                                                      + parameters.length + ", isRef.length="
                                                                      + isRef.length); }
    this.receiverClassName = receiverClassName;
    this.receiverClassLoaderDesc = receiverClassLoaderDesc;
    this.receiverID = receiverID;
    this.parameters = parameters;
    this.isRef = isRef;
    this.methodName = methodDesc.substring(0, methodDesc.indexOf('('));
    this.paramDesc = methodDesc.substring(methodDesc.indexOf('('));
  }

  public String getMethodName() {
    return this.methodName;
  }

  public String getReceiverClassName() {
    return receiverClassName;
  }

  public String getReceiverClassLoaderDesc() {
    return receiverClassLoaderDesc;
  }

  public String getParameterDesc() {
    return paramDesc;
  }

  public long getReceiverID() {
    return receiverID;
  }

  public final boolean isRef(int index) {
    return isRef[index];
  }

  public final Object[] getParameters() {
    // need to actually read the "parameters" array so that things are resolved correctly
    Object[] rv = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      rv[i] = parameters[i];
    }
    return rv;
  }
  
  public String toString() {
    return super.toString() + "[" + receiverClassName + ";  " + receiverID + ";  " + methodName + ";  " + paramDesc + ";  " + receiverClassLoaderDesc + "]";
  }

}
