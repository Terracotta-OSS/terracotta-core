package com.tc.admin.dso;

public class ClassTreeBranch extends ClassTreeTwig {
  private Integer instanceCount;

  public ClassTreeBranch(String name) {
    super(name);
  }

  public int getInstanceCount() {
    if(instanceCount == null) {
      ClassesHelper helper = ClassesHelper.getHelper();
      instanceCount = new Integer(helper.getInstanceCount(this));
    }

    return instanceCount.intValue();
  }
}
