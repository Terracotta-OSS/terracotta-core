/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.api;

/**
 * Dec 6, 2004: A logical action :-)
 */
public class LogicalAction {
  private final int      method;
  private final Object[] parameters;

  public LogicalAction(int method, Object[] parameters) {
    this.method = method;
    this.parameters = parameters;
  }

  public int getMethod() {
    return method;
  }

  public Object[] getParameters() {
    return parameters;
  }

}