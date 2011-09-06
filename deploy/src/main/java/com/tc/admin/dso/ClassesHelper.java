/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;
import com.tc.admin.common.XTreeNode;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;

public class ClassesHelper extends BaseHelper {
  private static final ClassesHelper helper = new ClassesHelper();
  private Icon                       classesIcon;

  private ClassesHelper() {/**/
  }

  public static ClassesHelper getHelper() {
    return helper;
  }

  public Icon getClassesIcon() {
    if (classesIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "class_obj.gif");
      if (url != null) {
        classesIcon = new ImageIcon(url);
      }
    }
    return classesIcon;
  }

  ClassTreeBranch testGetBranch(XTreeNode parent, String name) {
    XTreeNode child;

    for (int i = 0; i < parent.getChildCount(); i++) {
      child = (XTreeNode) parent.getChildAt(i);
      if (child instanceof ClassTreeBranch && name.equals(((ClassTreeBranch) child).getName())) { return (ClassTreeBranch) child; }
    }
    parent.add(child = new ClassTreeBranch(name));

    return (ClassTreeBranch) child;
  }

  ClassTreeLeaf testGetLeaf(XTreeNode parent, String name) {
    XTreeNode child;

    for (int i = 0; i < parent.getChildCount(); i++) {
      child = (XTreeNode) parent.getChildAt(i);
      if (child instanceof ClassTreeLeaf && name.equals(((ClassTreeLeaf) child).getName())) { return (ClassTreeLeaf) child; }
    }
    parent.add(child = new ClassTreeLeaf(name));

    return (ClassTreeLeaf) child;
  }

  int getInstanceCount(XTreeNode node) {
    int count = 0;
    int childCount = node.getChildCount();
    ClassTreeNode child;

    for (int i = 0; i < childCount; i++) {
      child = (ClassTreeNode) node.getChildAt(i);
      count += child.getInstanceCount();
    }

    return count;
  }

  public String getFullName(XTreeNode node) {
    StringBuffer sb = new StringBuffer();
    TreeNode[] path = node.getPath();
    ClassTreeNode child;

    for (int i = 1; i < path.length; i++) {
      child = (ClassTreeNode) path[i];
      sb.append(child.getName());
      if (i < path.length - 1) {
        sb.append(".");
      }
    }

    return sb.toString();
  }
}
