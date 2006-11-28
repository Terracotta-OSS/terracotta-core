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
