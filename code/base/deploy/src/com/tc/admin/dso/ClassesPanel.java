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
import com.tc.stats.DSOClassInfo;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

public class ClassesPanel extends XContainer {
  private ConnectionContext m_cc;
  private ClassesTable      m_table;
  private XTree             m_tree;
  private ClassesTreeMap    m_treeMap;

  private static final String REFRESH = "Refresh";

  public ClassesPanel(ConnectionContext cc) {
    super();

    AdminClientContext cntx = AdminClient.getContext();

    load((ContainerResource)cntx.topRes.getComponent("ClassesPanel"));

    m_cc = cc;

    DSOClassInfo[] classInfo = ClassesHelper.getHelper().getClassInfo(m_cc);

    m_table = (ClassesTable)findComponent("ClassTable");
    m_table.setClassInfo(classInfo);

    m_tree = (XTree)findComponent("ClassTree");
    m_tree.setShowsRootHandles(true);
    m_tree.setModel(new ClassTreeModel(classInfo));

    m_treeMap = (ClassesTreeMap)findComponent("ClassesTreeMap");
    m_treeMap.setModel((ClassTreeModel)m_tree.getModel());

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    getActionMap().put(REFRESH, new RefreshAction());
    getInputMap().put(ks, REFRESH);
  }

  public class RefreshAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  public void refresh() {
    AdminClientContext acc = AdminClient.getContext();

    acc.controller.setStatus(acc.getMessage("dso.classes.refreshing"));
    acc.controller.block();

    DSOClassInfo[] classInfo = ClassesHelper.getHelper().getClassInfo(m_cc);

    m_table.setClassInfo(classInfo);

    ((ClassTreeModel)m_tree.getModel()).setClassInfo(classInfo);

    m_treeMap.setModel((ClassTreeModel)m_tree.getModel());

    acc.controller.clearStatus();
    acc.controller.unblock();
  }
}
