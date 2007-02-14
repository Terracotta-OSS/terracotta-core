/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XRootNode;

import com.tc.stats.DSOClassInfo;

import com.tc.admin.AdminClient;

public class ClassTreeRoot extends XRootNode implements ClassTreeNode {
  private Integer instanceCount;

  private static final String NAME =
    AdminClient.getContext().getMessage("dso.allClasses");

  ClassTreeRoot(DSOClassInfo[] classInfo) {
    super();

    if(classInfo != null) {
      setClassInfo(classInfo);
    }
  }

  public void setClassInfo(DSOClassInfo[] classInfo) {
    setUserObject(classInfo);

    tearDownChildren();

    if(classInfo != null) {
      for(int i = 0; i < classInfo.length; i++) {
        String        className = classInfo[i].getClassName();
        String[]      names     = className.split("\\.");
        ClassTreeNode node      = this;
  
        for(int j = 0; j < names.length-1; j++) {
          node = node.testGetBranch(names[j]);
        }
  
        ClassTreeLeaf leaf = node.testGetLeaf(names[names.length-1]);
        leaf.setInstanceCount(classInfo[i].getInstanceCount());
      }
    }
  }

  DSOClassInfo[] getInfo() {
    return (DSOClassInfo[])getUserObject();
  }

  public String getName() {
    return NAME;
  }

  public String getFullName() {
    return getName();
  }

  public int getInstanceCount() {
    if(instanceCount == null) {
      ClassesHelper helper = ClassesHelper.getHelper();
      instanceCount = new Integer(helper.getInstanceCount(this));
    }

    return instanceCount.intValue();
  }

  public ClassTreeBranch testGetBranch(String name) {
    ClassesHelper helper = ClassesHelper.getHelper();
    return helper.testGetBranch(this, name);
  }

  public ClassTreeLeaf testGetLeaf(String name) {
    ClassesHelper helper = ClassesHelper.getHelper();
    return helper.testGetLeaf(this, name);
  }

  public String toString() {
    return getName() + " (" + getInstanceCount() + ")";
  }
}
