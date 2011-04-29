/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeCellRenderer;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModelElement;
import com.tc.admin.model.IClusterNode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class BasicObjectSetPanel extends XContainer implements PropertyChangeListener {
  private final XTree tree;

  public BasicObjectSetPanel() {
    super(new BorderLayout());

    tree = new XTree();
    tree.setCellRenderer(new BasicObjectCellRenderer());
    tree.setShowsRootHandles(true);
    tree.setVisibleRowCount(10);
    add(new JScrollPane(tree), BorderLayout.CENTER);
  }

  public BasicObjectSetPanel(IAdminClientContext adminClientContext, IBasicObject[] roots) {
    this(adminClientContext, null, roots);
  }

  public BasicObjectSetPanel(IAdminClientContext adminClientContext, IClient client, IBasicObject[] roots) {
    this();
    setObjects(adminClientContext, client, roots);
  }

  XTree getTree() {
    return tree;
  }

  private static class BasicObjectCellRenderer extends XTreeCellRenderer {
    @Override
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

  public void setObjects(IAdminClientContext adminClientContext, IClient client, IBasicObject[] roots) {
    tree.setModel(new BasicObjectTreeModel(adminClientContext, client, roots));
    tree.revalidate();
    tree.repaint();
    if (client != null) {
      client.addPropertyChangeListener(this);
    }
  }

  public void clearModel() {
    tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
    tree.revalidate();
    tree.repaint();
  }

  public BasicObjectTreeModel getObjectTreeModel() {
    return (BasicObjectTreeModel) tree.getModel();
  }

  public void refresh() {
    getObjectTreeModel().refresh();
  }

  public void add(IBasicObject root) {
    getObjectTreeModel().add(root);
    tree.revalidate();
    tree.repaint();
  }

  @Override
  public void tearDown() {
    BasicObjectTreeModel treeModel = getObjectTreeModel();
    treeModel.tearDown();

    super.tearDown();
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModelElement.PROP_READY.equals(prop)) {
      IClusterNode clusterNode = (IClusterNode) evt.getSource();
      final boolean isReady = clusterNode.isReady();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (!isReady) {
            clearModel();
          }
        }
      });
    }
  }
}
