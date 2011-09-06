/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import treemap.TMModelNode;
import treemap.TMModelUpdater;

import com.tc.admin.common.XTreeNode;

import java.util.Enumeration;
import java.util.Vector;

public class ClassesModelNode implements TMModelNode {
  private ClassTreeModel treeModel;

  public ClassesModelNode(ClassTreeModel treeModel) {
    this.treeModel = treeModel;
  }

  public Object getRoot() {
    return treeModel.getRoot();
  }

  public Enumeration children(Object node) {
    Vector children = new Vector();

    if (node instanceof XTreeNode) {
      XTreeNode treeNode = (XTreeNode) node;
      int childCount = treeNode.getChildCount();

      for (int i = 0; i < childCount; i++) {
        children.add(treeNode.getChildAt(i));
      }
    }

    return children.elements();
  }

  public boolean isLeaf(Object node) {
    return node instanceof ClassTreeLeaf;
  }

  public void setUpdater(TMModelUpdater updater) {/**/
  }
}
