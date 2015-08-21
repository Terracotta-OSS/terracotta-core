/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.api;

import java.io.Serializable;

public class ClassInfo implements Serializable {
  private final String className;
  private final int    instanceCount;

  public ClassInfo(String className, int instanceCount) {
    this.className = className;
    this.instanceCount = instanceCount;
  }

  public String getClassName() {
    return className;
  }

  public int getInstanceCount() {
    return instanceCount;
  }
}
