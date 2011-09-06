/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

public interface ClassTreeNode {
  String getName();

  String getFullName();

  int getInstanceCount();

  ClassTreeBranch testGetBranch(String name);

  ClassTreeLeaf testGetLeaf(String name);
}
