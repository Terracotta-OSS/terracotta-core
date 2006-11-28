package com.tc.admin.dso;

public interface ClassTreeNode {
  String          getName();
  String          getFullName();
  int             getInstanceCount();
  ClassTreeBranch testGetBranch(String name);
  ClassTreeLeaf   testGetLeaf(String name);
}
