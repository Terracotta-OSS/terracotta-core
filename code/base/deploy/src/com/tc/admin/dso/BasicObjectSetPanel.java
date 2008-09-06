/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeCellRenderer;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterNode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class BasicObjectSetPanel extends XContainer {
  private XTree m_tree;

  public BasicObjectSetPanel() {
    super(new BorderLayout());

    m_tree = new XTree();
    m_tree.setCellRenderer(new BasicObjectCellRenderer());
    m_tree.setShowsRootHandles(true);
    m_tree.setVisibleRowCount(10);
    add(new JScrollPane(m_tree), BorderLayout.CENTER);
  }

  public BasicObjectSetPanel(IClusterNode clusterNode, IBasicObject[] roots) {
    this();
    setObjects(clusterNode, roots);
  }

  XTree getTree() {
    return m_tree;
  }

  private static class BasicObjectCellRenderer extends XTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean focused) {
      Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
      if (value instanceof BasicObjectNode) {
        boolean isResident = ((BasicObjectNode) value).isResident();
        if (!isResident) {
          setForeground(Color.lightGray);
        }
      }
      return comp;
    }
  }

  public void setObjects(IClusterNode clusterNode, IBasicObject[] roots) {
    m_tree.setModel(new BasicObjectTreeModel(clusterNode, roots));
    m_tree.revalidate();
    m_tree.repaint();
  }

  public void clearModel() {
    m_tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
    m_tree.revalidate();
    m_tree.repaint();
  }

  public BasicObjectTreeModel getObjectTreeModel() {
    return (BasicObjectTreeModel) m_tree.getModel();
  }

  public void refresh() {
    getObjectTreeModel().refresh();
  }

  public void add(IBasicObject root) {
    getObjectTreeModel().add(root);
    m_tree.revalidate();
    m_tree.repaint();
  }

  public void tearDown() {
    super.tearDown();
    m_tree = null;
  }
}
