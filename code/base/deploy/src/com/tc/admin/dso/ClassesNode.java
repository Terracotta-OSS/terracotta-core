/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ClusterNode;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

public class ClassesNode extends ComponentNode {
  protected ClusterNode       m_clusterNode;
  protected ClassesPanel      m_classesPanel;
  protected JPopupMenu        m_popupMenu;
  protected RefreshAction     m_refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";

  public ClassesNode(ClusterNode clusterNode) {
    super();
    m_clusterNode = clusterNode;
    setLabel(AdminClient.getContext().getMessage("dso.classes"));
    initMenu();
  }

  protected ClassesPanel createClassesPanel() {
    return new ClassesPanel(this);
  }

  public Component getComponent() {
    if (m_classesPanel == null) {
      m_classesPanel = createClassesPanel();
    }
    return m_classesPanel;
  }

  IClusterModel getClusterModel() {
    return m_clusterNode.getClusterModel();
  }

  private void initMenu() {
    m_refreshAction = new RefreshAction();

    m_popupMenu = new JPopupMenu("Classes Actions");
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
    if (m_classesPanel != null) {
      m_classesPanel.refresh();
    }
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

  public void tearDown() {
    if (m_classesPanel != null) {
      m_classesPanel.tearDown();
    }

    super.tearDown();

    m_clusterNode = null;
    m_classesPanel = null;
    m_popupMenu = null;
    m_refreshAction = null;
  }
}
