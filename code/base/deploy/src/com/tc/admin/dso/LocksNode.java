/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class LocksNode extends ComponentNode implements PopupMenuListener {
  private ConnectionContext           m_cc;
  private JPopupMenu                  m_popupMenu;
  private JCheckBoxMenuItem           m_enableStatsToggle;
  private EnableAllClientTracesAction  m_enableAllClientTracesAction;
  private DisableAllClientTracesAction m_disableAllClientTracesAction;

  private static final String         REFRESH_ACTION = "RefreshAction";

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
    m_popupMenu.add(m_enableStatsToggle = new JCheckBoxMenuItem(new StatsEnabledAction()));
    m_popupMenu.add(new JMenuItem(m_enableAllClientTracesAction = new EnableAllClientTracesAction()));
    m_popupMenu.add(new JMenuItem(m_disableAllClientTracesAction = new DisableAllClientTracesAction()));

    addActionBinding(REFRESH_ACTION, refreshAction);

    m_popupMenu.addPopupMenuListener(this);
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

  public class StatsEnabledAction extends XAbstractAction {
    StatsEnabledAction() {
      super("Stats enabled");
    }

    public void actionPerformed(ActionEvent ae) {
      setLockStatisticsEnabled(m_enableStatsToggle.isSelected());
    }
  }

  public class EnableAllClientTracesAction extends XAbstractAction {
    EnableAllClientTracesAction() {
      super("Enable all client traces");
    }

    public void actionPerformed(ActionEvent ae) {
      setAllStatsEnabled(true);
    }
  }

  public class DisableAllClientTracesAction extends XAbstractAction {
    DisableAllClientTracesAction() {
      super("Disable all client traces");
    }

    public void actionPerformed(ActionEvent ae) {
      setAllStatsEnabled(false);
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

  public void popupMenuCanceled(PopupMenuEvent e) {/**/
  }

  public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {/**/
  }

  public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    boolean lockStatsEnabled = isLockStatisticsEnabled();
    m_enableStatsToggle.setSelected(lockStatsEnabled);
    m_enableAllClientTracesAction.setEnabled(lockStatsEnabled);
    m_disableAllClientTracesAction.setEnabled(lockStatsEnabled);
  }

  private boolean isLockStatisticsEnabled() {
    return ((LocksPanel) getComponent()).isLockStatisticsEnabled();
  }

  private void setLockStatisticsEnabled(boolean lockStatsEnabled) {
    ((LocksPanel) getComponent()).setLockStatisticsEnabled(lockStatsEnabled);
  }

  private void setAllStatsEnabled(boolean allStatsEnabled) {
    ((LocksPanel) getComponent()).setAllClientTracesEnabled(allStatsEnabled);
  }

  public void tearDown() {
    super.tearDown();

    m_cc = null;
    m_popupMenu = null;
  }
}
