/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.api;

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
