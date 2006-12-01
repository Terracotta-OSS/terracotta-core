/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;

public class LocksNode extends ComponentNode {
  private ConnectionContext m_cc;
  private JPopupMenu        m_popupMenu;

  private static final String REFRESH_ACTION          = "RefreshAction";
  //private static final String DETECT_DEADLOCKS_ACTION = "DetectDeadlocksAction";

  public LocksNode(ConnectionContext cc) {
    super();

    m_cc = cc;

    setLabel(AdminClient.getContext().getMessage("dso.locks"));
    setComponent(new LocksPanel(m_cc));
    
    initMenu();
  }

  private void initMenu() {
    RefreshAction         refreshAction         = new RefreshAction();
    //DetectDeadlocksAction detectDeadlocksAction = new DetectDeadlocksAction();

    m_popupMenu = new JPopupMenu("Lock Actions");
    m_popupMenu.add(refreshAction);
    //m_popupMenu.add(detectDeadlocksAction);

    addActionBinding(REFRESH_ACTION, refreshAction);
    //addActionBinding(DETECT_DEADLOCKS_ACTION, detectDeadlocksAction);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  public Icon getIcon() {
    return LocksHelper.getHelper().getLocksIcon();
  }

  public void refresh() {
    ((LocksPanel)getComponent()).refresh();
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super();

      AdminClientContext acc = AdminClient.getContext();
      
      setName(acc.getMessage("refresh.name"));
      setSmallIcon(LocksHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  /*
  private void detectDeadlocks() {
    LocksHelper.getHelper().detectDeadlocks(m_cc);
  }

  private class DetectDeadlocksAction extends XAbstractAction {
    private DetectDeadlocksAction() {
      super();

      AdminClientContext acc = AdminClient.getContext();
      
      setName(acc.getMessage("dso.deadlocks.detect"));
      setSmallIcon(LocksHelper.getHelper().getDetectDeadlocksIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc = AdminClient.getContext();

      acc.controller.setStatus(acc.getMessage("dso.deadlocks.detecting"));
      try {
        detectDeadlocks();
      }
      catch(Throwable t) {
        t.printStackTrace();
      }
      acc.controller.clearStatus();
    }
  }
  */
  
  public void tearDown() {
    super.tearDown();

    m_cc        = null;
    m_popupMenu = null;
  }
}
