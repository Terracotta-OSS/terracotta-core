/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
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
