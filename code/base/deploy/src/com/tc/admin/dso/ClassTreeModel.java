package com.tc.admin.dso;

import com.tc.admin.common.XTreeModel;

import com.tc.stats.DSOClassInfo;

public class ClassTreeModel extends XTreeModel {
  public ClassTreeModel(DSOClassInfo[] classInfo) {
    super(new ClassTreeRoot(classInfo));
  }

  public void setClassInfo(DSOClassInfo[] classInfo) {
    ((ClassTreeRoot)getRoot()).setClassInfo(classInfo);
    reload();
  }
}
