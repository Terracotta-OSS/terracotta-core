/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XTreeNode;

public abstract class ClassTreeTwig extends XTreeNode implements ClassTreeNode {
  public ClassTreeTwig(String name) {
    super(name);
  }

  public String getName() {
    return (String) getUserObject();
  }

  public String getFullName() {
    return ClassesHelper.getHelper().getFullName(this);
  }

  public abstract int getInstanceCount();

  public ClassTreeBranch testGetBranch(String name) {
    return ClassesHelper.getHelper().testGetBranch(this, name);
  }

  public ClassTreeLeaf testGetLeaf(String name) {
    return ClassesHelper.getHelper().testGetLeaf(this, name);
  }

  public String toString() {
    return getName() + " (" + getInstanceCount() + ")";
  }
}
