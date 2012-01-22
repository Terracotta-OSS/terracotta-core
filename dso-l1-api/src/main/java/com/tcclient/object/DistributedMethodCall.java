/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  /**
   * Construct DMI call
   * 
   * @param receiver Receiver object, should never be null
   * @param parameters Parameters to method call, should never be null
   * @param methodName Receiver method name, should never be null
   * @param paramDesc Description of parameter types on method call, should never be null
   */
  public DistributedMethodCall(final Object receiver, final Object[] parameters, final String methodName,
                               final String paramDesc) {
    this.receiver = receiver;
    this.parameters = parameters;
    this.methodName = methodName;
    this.paramDesc = paramDesc;
  }

  /**
   * @return Receiver object
   */
  public Object getReceiver() {
    return receiver;
  }

  /**
   * @return Method name
   */
  public String getMethodName() {
    return this.methodName;
  }

  /**
   * @return Parameters description
   */
  public String getParameterDesc() {
    return paramDesc;
  }

  /**
   * @return Parameter values (unresolved if not literals)
   */
  public final Object[] getParametersUnresolved() {
    return parameters;
  }

  @Override
  public String toString() {
    return receiver.getClass().getName() + "." + methodName + paramDesc;
  }
}
