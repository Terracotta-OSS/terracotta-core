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
  private ConnectionContext   m_cc;
  private JPopupMenu          m_popupMenu;

  private static final String REFRESH_ACTION = "RefreshAction";

  public LocksNode(ConnectionContext cc) {
    super();

    m_cc = cc;

    setLabel(AdminClient.getContext().getMessage("dso.locks"));
    setComponent(new LocksPanel(m_cc));

    initMenu();
  }

  private void initMenu() {
    RefreshAction refreshAction = new RefreshAction();

    m_popupMenu = new JPopupMenu("Lock Actions");
    m_popupMenu.add(refreshAction);
    m_popupMenu.add(new EnableStatsAction());
    m_popupMenu.add(new DisableStatsAction());
    
    addActionBinding(REFRESH_ACTION, refreshAction);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  public Icon getIcon() {
    return LocksHelper.getHelper().getLocksIcon();
  }

  public void refresh() {
    ((LocksPanel) getComponent()).refresh();
  }

  public class DisableStatsAction extends XAbstractAction {
    DisableStatsAction() {
      super("Disable stats");
    }

    public void actionPerformed(ActionEvent ae) {
      ((LocksPanel) getComponent()).disableLockStatistics();
    }
  }
  
  public class EnableStatsAction extends XAbstractAction {
    EnableStatsAction() {
      super("Enable stats");
    }

    public void actionPerformed(ActionEvent ae) {
      ((LocksPanel) getComponent()).enableLockStatistics();
    }
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

  public void tearDown() {
    super.tearDown();

    m_cc = null;
    m_popupMenu = null;
  }
}
