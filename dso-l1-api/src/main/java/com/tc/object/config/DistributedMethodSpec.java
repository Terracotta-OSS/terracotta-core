/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

public class DistributedMethodSpec {

  private final String  methodExpression;
  private final boolean runOnAllNodes;

  public DistributedMethodSpec(final String methodExpression, final boolean runOnAllNodes) {
    this.methodExpression = methodExpression;
    this.runOnAllNodes = runOnAllNodes;
  }

  public String getMethodExpression() {
    return methodExpression;
  }

  public boolean runOnAllNodes() {
    return runOnAllNodes;
  }

  public String toString() {
    return "DistributedMethodSpec{methodExpression=" + methodExpression + ", runOnAllNodes=" + runOnAllNodes + "}";
  }
}
