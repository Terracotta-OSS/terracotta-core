/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.object.LogicalOperation;
import java.util.Arrays;

/**
 * A logical action representing a method invocation to be replayed elsewhere. The method signatures can only come from
 * a limited set, which are defined in {@link com.tc.object.SerializationUtil}.
 */
public class LogicalAction {
  private final LogicalOperation method;
  private final Object[] parameters;
  private final LogicalChangeID id;

  /**
   * Construct a logical action with the method identifier and parameter values
   * 
   * @param method Method identifier, as defined in {@link com.tc.object.SerializationUtil}
   * @param parameters Parameters to the method call, may be empty but not null
   */
  public LogicalAction(LogicalOperation method, Object[] parameters) {
    this(method, parameters, LogicalChangeID.NULL_ID);
  }

  /**
   * Construct a logical action with the method identifier and parameter values
   * 
   * @param method Method identifier, as defined in {@link com.tc.object.SerializationUtil}
   * @param parameters Parameters to the method call, may be empty but not null
   */
  public LogicalAction(LogicalOperation method, Object[] parameters, LogicalChangeID id) {
    this.method = method;
    this.parameters = parameters;
    this.id = id;
  }

  /**
   * Get method identifier
   * 
   * @return Method identifier, as defined in {@link com.tc.object.SerializationUtil}
   */
  public LogicalOperation getLogicalOperation() {
    return method;
  }

  /**
   * Get parameter values
   * 
   * @return The parameters, never null
   */
  public Object[] getParameters() {
    return parameters;
  }


  @Override
  public String toString() {
    return "LogicalAction [method=" + method + ", parameters=" + Arrays.toString(parameters) + "]";
  }

  public LogicalChangeID getLogicalChangeID() {
    return id;
  }
}