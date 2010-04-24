/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.api;

/**
 * A logical action representing a method invocation to be replayed elsewhere.  The method
 * signatures can only come from a limited set, which are defined in {@link com.tc.object.SerializationUtil}.
 */
public class LogicalAction {
  private final int      method;
  private final Object[] parameters;

  /**
   * Construct a logical action with the method identifier and parameter values
   * @param method Method identifier, as defined in {@link com.tc.object.SerializationUtil}
   * @param parameters Parameters to the method call, may be empty but not null
   */
  public LogicalAction(int method, Object[] parameters) {
    this.method = method;
    this.parameters = parameters;
  }

  /**
   * Get method identifier
   * @return Method identifier, as defined in {@link com.tc.object.SerializationUtil}
   */
  public int getMethod() {
    return method;
  }

  /**
   * Get parameter values
   * @return The parameters, never null
   */
  public Object[] getParameters() {
    return parameters;
  }

}