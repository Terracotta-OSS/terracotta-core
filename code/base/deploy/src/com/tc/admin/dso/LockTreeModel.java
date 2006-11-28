package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

import com.tc.objectserver.lockmanager.api.LockMBean;

public class LockTreeModel extends XTreeModel {
  private ConnectionContext m_cc;

  public LockTreeModel(ConnectionContext cc) {
    super();
    m_cc = cc;
  }

  public void setLocks(LockMBean[] locks) {
    XTreeNode rootNode = (XTreeNode)getRoot();

    rootNode.tearDownChildren();

    for(int i = 0; i < locks.length; i++) {
      insertNodeInto(new LockTreeNode(m_cc, locks[i]), rootNode, i);
    }

    nodeStructureChanged(rootNode);
  }
}
