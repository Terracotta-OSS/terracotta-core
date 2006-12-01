/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

public class ClassesNode extends ComponentNode {
  private JPopupMenu    m_popupMenu;
  private RefreshAction m_refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";

  public ClassesNode(ConnectionContext cc) {
    super();

    setLabel(AdminClient.getContext().getMessage("dso.classes"));
    setComponent(new ClassesPanel(cc));

    initMenu();
  }

  private void initMenu() {
    m_refreshAction = new RefreshAction();

    m_popupMenu = new JPopupMenu("Roots Actions");
    m_popupMenu.add(m_refreshAction);

    addActionBinding(REFRESH_ACTION, m_refreshAction);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  public Icon getIcon() {
    return ClassesHelper.getHelper().getClassesIcon();
  }

  public void refresh() {
    ((ClassesPanel)getComponent()).refresh();
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super();

      setName(AdminClient.getContext().getMessage("refresh.name"));
      setSmallIcon(ClassesHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  public void nodeClicked(MouseEvent me) {
    m_refreshAction.actionPerformed(null);
  }
}
