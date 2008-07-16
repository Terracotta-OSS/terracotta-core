/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeCellRenderer;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterNode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class BasicObjectSetPanel extends XContainer {
  private XTree m_tree;

  public BasicObjectSetPanel() {
    super(new BorderLayout());

    m_tree = new XTree();
    m_tree.setCellRenderer(new BasicObjectCellRenderer());
    m_tree.setShowsRootHandles(true);
    ObjectTreeEventListener treeListener = new ObjectTreeEventListener();
    m_tree.addMouseListener(treeListener);
    m_tree.addTreeSelectionListener(treeListener);
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
          m_label.setForeground(Color.lightGray);
        }
      }
      return comp;
    }
  }

  private class ObjectTreeEventListener extends MouseAdapter implements TreeSelectionListener {
    public void mouseClicked(MouseEvent me) {
      TreePath path = m_tree.getPathForLocation(me.getX(), me.getY());
      if (path != null) {
        m_tree.requestFocus();
        XTreeNode node = (XTreeNode) path.getLastPathComponent();
        if (node != null) {
          node.nodeClicked(me);
        }
      }
    }

    public void valueChanged(TreeSelectionEvent tse) {
      TreePath path = tse.getNewLeadSelectionPath();
      if (path != null) {
        m_tree.requestFocus();
        XTreeNode node = (XTreeNode) path.getLastPathComponent();
        if (node != null) {
          node.nodeSelected(tse);
        }
      }
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
