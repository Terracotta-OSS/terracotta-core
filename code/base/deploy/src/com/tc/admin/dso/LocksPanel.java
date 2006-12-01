/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.ContainerResource;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTree;
import com.tc.objectserver.lockmanager.api.LockMBean;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

public class LocksPanel extends XContainer {
  private ConnectionContext m_cc;
  private LockMBean[]       m_locks;
  private XTree             m_lockTree;
  private LockTreeModel     m_lockTreeModel;

  private static final String REFRESH = "Refresh";

  public LocksPanel(ConnectionContext cc) {
    super();

    AdminClientContext cntx = AdminClient.getContext();

    load((ContainerResource)cntx.topRes.getComponent("LocksPanel"));

    m_cc       = cc;
    m_lockTree = (XTree)findComponent("LockTree");
    m_lockTree.setShowsRootHandles(true);
    m_lockTree.setModel(m_lockTreeModel = new LockTreeModel(m_cc));

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    getActionMap().put(REFRESH, new RefreshAction());
    getInputMap().put(ks, REFRESH);

    updateTreeModel();
  }

  public class RefreshAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  private void updateTreeModel() {
    try {
      m_locks = LocksHelper.getHelper().getLocks(m_cc);
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
      m_locks = new LockMBean[]{};
    }

    m_lockTreeModel.setLocks(m_locks);
    m_lockTree.expandAll();
  }

  public void refresh() {
    AdminClientContext acc = AdminClient.getContext();

    acc.controller.setStatus(acc.getMessage("dso.locks.refreshing"));
    updateTreeModel();
    acc.controller.clearStatus();
  }
}
