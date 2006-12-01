/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.ConnectionContext;

public class RootTreeModel extends XTreeModel {
  private ConnectionContext m_cc;

  public RootTreeModel(ConnectionContext cc, DSORoot[] roots) {
    super();

    m_cc = cc;

    XTreeNode rootNode = (XTreeNode)getRoot();

    if(roots != null && roots.length > 0) {
      for(int i = 0; i < roots.length; i++) {
        insertNodeInto(new RootTreeNode(m_cc, roots[i]), rootNode, i);
      }
    }
  }

  public void refresh() {
    XTreeNode rootNode = (XTreeNode)getRoot();

    for(int i = rootNode.getChildCount()-1; i >= 0; i--) {
      ((RootTreeNode)getChild(rootNode, i)).refresh();
    }
  }

  public void add(DSORoot dsoRoot) {
    XTreeNode parentNode = (XTreeNode)getRoot();
    int       index      = parentNode.getChildCount();

    insertNodeInto(new RootTreeNode(m_cc, dsoRoot), parentNode, index);
  }
}
