/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats;

import java.io.Serializable;

public class DSOClassInfo implements Serializable {
  private final String className;
  private final int    instanceCount;

  public DSOClassInfo(String className, int instanceCount) {
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
