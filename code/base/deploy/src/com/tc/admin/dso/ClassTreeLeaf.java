/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin.dso;

public class ClassTreeLeaf extends ClassTreeTwig {
  int instanceCount;

  public ClassTreeLeaf(String name) {
    super(name);
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public int getInstanceCount() {
    return instanceCount;
  }
}
