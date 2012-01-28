/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import treemap.TMComputeDrawAdapter;
import treemap.TMComputeSizeAdapter;
import treemap.TMView;
import treemap.TreeMap;

import com.tc.admin.common.XContainer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;

public class ClassesTreeMap extends XContainer {
  private TreeMap treeMap;

  public ClassesTreeMap() {
    super(new BorderLayout());
  }

  public void setModel(ClassTreeModel treeModel) {
    removeAll();

    treeMap = new TreeMap(new ClassesModelNode(treeModel));
    TMView view = treeMap.getView(new ClassesModelSize(), new ClassesModelDraw());
    view.setAlgorithm(TMView.SQUARIFIED);
    view.getAlgorithm().setBorderSize(14);

    add(view);
  }

  private static class ClassesModelSize extends TMComputeSizeAdapter {
    public boolean isCompatibleWithObject(Object node) {
      return true;
    }

    public float getSizeOfObject(Object node) {
      if (node instanceof ClassTreeNode) { return ((ClassTreeNode) node).getInstanceCount(); }
      return 0.0f;
    }
  }

  private static class ClassesModelDraw extends TMComputeDrawAdapter {
    private Color fillColor = new Color(255, 238, 105);

    public boolean isCompatibleWithObject(Object node) {
      return true;
    }

    public Paint getFillingOfObject(Object node) {
      return fillColor;
    }

    public String getTooltipOfObject(Object node) {
      if (node instanceof ClassTreeNode) {
        ClassTreeNode ctn = (ClassTreeNode) node;
        return "<html>" + ctn.getFullName() + "<p>" + ctn.getInstanceCount() + " instances";
      }
      return "";
    }

    public String getTitleOfObject(Object node) {
      if (node instanceof ClassTreeNode) { return ((ClassTreeNode) node).getName(); }
      return "";
    }

    public Paint getColorTitleOfObject(Object node) {
      return Color.black;
    }
  }
}
