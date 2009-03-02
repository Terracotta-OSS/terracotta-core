/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
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
  private ApplicationContext  appContext;
  protected IClusterModel     clusterModel;
  protected ClassesPanel      classesPanel;
  protected JPopupMenu        popupMenu;
  protected RefreshAction     refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";

  public ClassesNode(ApplicationContext appContext, IClusterModel clusterModel) {
    super();
    this.appContext = appContext;
    this.clusterModel = clusterModel;
    setLabel(appContext.getMessage("dso.classes"));
    initMenu();
  }

  protected ClassesPanel createClassesPanel() {
    return new ClassesPanel(appContext, clusterModel);
  }

  @Override
  public Component getComponent() {
    if (classesPanel == null) {
      appContext.block();
      classesPanel = createClassesPanel();
      appContext.unblock();
    }
    return classesPanel;
  }

  IClusterModel getClusterModel() {
    return clusterModel;
  }

  private void initMenu() {
    refreshAction = new RefreshAction();
    popupMenu = new JPopupMenu("Classes Actions");
    popupMenu.add(refreshAction);
    addActionBinding(REFRESH_ACTION, refreshAction);
  }

  @Override
  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  @Override
  public Icon getIcon() {
    return ClassesHelper.getHelper().getClassesIcon();
  }

  public void refresh() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null && theClusterModel.isReady() && classesPanel != null) {
      classesPanel.refresh();
    }
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super();
      setName(appContext.getMessage("refresh.name"));
      setSmallIcon(ClassesHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  @Override
  public void nodeClicked(MouseEvent me) {
    refreshAction.actionPerformed(null);
  }

  @Override
  public void tearDown() {
    if (classesPanel != null) {
      classesPanel.tearDown();
    }

    super.tearDown();

    appContext = null;
    clusterModel = null;
    classesPanel = null;
    popupMenu = null;
    refreshAction = null;
  }
}
