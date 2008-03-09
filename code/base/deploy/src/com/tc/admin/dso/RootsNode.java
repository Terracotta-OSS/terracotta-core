/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ClusterNode;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeModel;
import com.tc.stats.DSOMBean;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class RootsNode extends ComponentNode implements NotificationListener {
  private ClusterNode         m_clusterNode;
  private ConnectionContext   m_cc;
  private DSORoot[]           m_roots;
  private JPopupMenu          m_popupMenu;
  private RefreshAction       m_refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";

  public RootsNode(ClusterNode clusterNode) throws Exception {
    super();

    m_clusterNode = clusterNode;
    init();
  }

  private void init() throws Exception {
    m_cc = m_clusterNode.getConnectionContext();
    m_roots = getRoots();

    initMenu();

    AdminClientContext acc = AdminClient.getContext();
    String label = acc.getMessage("dso.roots");
    RootsPanel panel = new RootsPanel(m_cc, m_roots);

    panel.setNode(this);
    setLabel(label);
    setComponent(panel);

    for (int i = 0; i < m_roots.length; i++) {
      insert(new RootNode(m_cc, m_roots[i]), i);
    }

    ObjectName dso = DSOHelper.getHelper().getDSOMBean(m_cc);
    m_cc.addNotificationListener(dso, this);
  }

  public void newConnectionContext() {
    try {
      init();
    } catch (Exception e) {/**/
    }
  }

  public DSORoot[] getRoots() throws Exception {
    return RootsHelper.getHelper().getRoots(m_cc);
  }

  public DSORoot getRoot(int index) {
    return m_roots != null ? m_roots[index] : null;
  }

  public int getRootCount() {
    return m_roots != null ? m_roots.length : 0;
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
    return RootsHelper.getHelper().getRootsIcon();
  }

  public void refresh() throws Exception {
    AdminClientContext acc = AdminClient.getContext();
    boolean expanded = acc.controller.isExpanded(this);

    tearDownChildren();

    m_roots = getRoots();
    for (int i = 0; i < m_roots.length; i++) {
      m_roots[i].refresh();
      insert(new RootNode(m_cc, m_roots[i]), i);
    }
    ((RootsPanel) getComponent()).setRoots(m_roots);

    getModel().nodeStructureChanged(this);
    if (expanded) {
      acc.controller.expand(this);
    }
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super();

      setName(AdminClient.getContext().getMessage("refresh.name"));
      setSmallIcon(RootsHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc = AdminClient.getContext();

      acc.controller.setStatus(acc.getMessage("dso.roots.refreshing"));
      acc.controller.block();

      try {
        refresh();
      } catch (Exception e) {
        AdminClient.getContext().log(e);
      }

      acc.controller.unblock();
      acc.controller.clearStatus();
    }
  }

  public void nodeClicked(MouseEvent me) {
    m_refreshAction.actionPerformed(null);
  }

  private boolean haveRoot(ObjectName objectName) {
    if (m_roots == null) return false;
    for (DSORoot root : m_roots) {
      if (root.getObjectName().equals(objectName)) { return true; }
    }
    return false;
  }

  public void handleNotification(final Notification notice, Object handback) {
    String type = notice.getType();

    if (DSOMBean.ROOT_ADDED.equals(type)) {
      final ObjectName rootObjectName = (ObjectName) notice.getSource();
      if (haveRoot(rootObjectName)) return;

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          AdminClientContext acc = AdminClient.getContext();

          acc.setStatus(acc.getMessage("dso.root.retrieving"));

          DSORoot root = new DSORoot(m_cc, rootObjectName);
          ArrayList<DSORoot> list = new ArrayList<DSORoot>(Arrays.asList(m_roots));

          list.add(root);
          m_roots = list.toArray(new DSORoot[] {});

          RootNode rn = new RootNode(m_cc, root);
          XTreeModel model = getModel();
          if (model != null) {
            model.insertNodeInto(rn, RootsNode.this, getChildCount());
          } else {
            RootsNode.this.add(rn);
          }

          ((RootsPanel) getComponent()).add(root);

          acc.setStatus(acc.getMessage("dso.root.new") + root);
        }
      });
    }
  }

  public void tearDown() {
    try {
      ObjectName dso = DSOHelper.getHelper().getDSOMBean(m_cc);
      if (dso != null) {
        m_cc.removeNotificationListener(dso, this);
      }
    } catch (Exception e) {/**/
    }

    m_cc = null;
    m_roots = null;
    m_popupMenu = null;
    m_refreshAction = null;

    super.tearDown();
  }
}
