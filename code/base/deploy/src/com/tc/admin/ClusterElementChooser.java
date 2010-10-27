/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.TreeComboBox;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IClusterModelElement;

import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public abstract class ClusterElementChooser extends TreeComboBox {
  public ClusterElementChooser(IClusterModel clusterModel, ActionListener listener) {
    super(listener);
  }

  @Override
  public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    throw new ExpandVetoException(event);
  }

  @Override
  protected JTree createTree() {
    return new XTree();
  }

  @Override
  protected TreeModel createTreeModel() {
    return new XTreeModel();
  }

  @Override
  protected boolean acceptPath(TreePath path) {
    Object o = path.getLastPathComponent();
    return (o instanceof ClusterElementNode);
  }

  protected abstract XTreeNode[] createTopLevelNodes();

  public void setupTreeModel() {
    XRootNode root = (XRootNode) treeModel.getRoot();
    root.tearDownChildren();
    for (XTreeNode child : createTopLevelNodes()) {
      root.addChild(child);
    }
    root.nodeStructureChanged();
    reSetToLastSelectedPath();
  }

  public TreePath getPath(IClusterModelElement clusterElement) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
    Enumeration e = root.preorderEnumeration();
    while (e.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
      if (node instanceof ClusterElementNode) {
        IClusterModelElement cme = ((ClusterElementNode) node).getClusterElement();
        if (clusterElement.equals(cme)) { return new TreePath(node.getPath()); }
      }
    }
    return null;
  }

  @Override
  public void tearDown() {
    ((XTreeModel) treeModel).tearDown();
    super.tearDown();
  }
}
