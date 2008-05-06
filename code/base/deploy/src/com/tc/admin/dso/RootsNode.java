/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Component;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ClusterNode;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.stats.DSOMBean;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class RootsNode extends ComponentNode implements NotificationListener {
  protected AdminClientContext m_acc;
  protected ClusterNode        m_clusterNode;
  protected ConnectionContext  m_cc;
  protected DSORoot[]          m_roots;
  protected RootsPanel         m_rootsPanel;
  protected JPopupMenu         m_popupMenu;
  protected RefreshAction      m_refreshAction;

  private static final String  REFRESH_ACTION = "RefreshAction";

  public RootsNode(ClusterNode clusterNode) throws Exception {
    super();
    m_acc = AdminClient.getContext();
    m_clusterNode = clusterNode;
    init();
  }

  private void init() {
    m_roots = new DSORoot[0];
    for (int i = getChildCount() - 1; i >= 0; i--) {
      m_acc.controller.remove((XTreeNode) getChildAt(i));
    }
    if(m_rootsPanel != null) {
      m_rootsPanel.clearModel();
    }
    m_acc.executorService.execute(new InitWorker());
  }

  private class InitWorker extends BasicWorker<DSORoot[]> {
    private InitWorker() {
      super(new Callable<DSORoot[]>() {
        public DSORoot[] call() throws Exception {
          m_cc = m_clusterNode.getConnectionContext();
          ObjectName dso = DSOHelper.getHelper().getDSOMBean(m_cc);
          m_cc.addNotificationListener(dso, RootsNode.this);
          return getRoots();
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        m_roots = getResult();
        initMenu();
        setLabel(m_acc.getMessage("dso.roots"));
        for (int i = 0; i < m_roots.length; i++) {
          insert(new RootNode(m_cc, m_roots[i]), i);
        }
        m_acc.controller.nodeStructureChanged(RootsNode.this);
        if(m_rootsPanel != null) {
          m_rootsPanel.setup(m_cc, m_roots);
        }
      }
    }
  }

  protected RootsPanel createRootsPanel() {
    return new RootsPanel(m_cc, m_roots);
  }

  public Component getComponent() {
    if (m_rootsPanel == null) {
      m_rootsPanel = createRootsPanel();
      m_rootsPanel.setNode(this);
    }
    return m_rootsPanel;
  }

  public void newConnectionContext() {
    init();
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

  private class RefreshWorker extends BasicWorker<Void> {
    private boolean isExpanded;

    private RefreshWorker(final boolean isExpanded) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          tearDownChildren();
          m_roots = getRoots();
          for (int i = 0; i < m_roots.length; i++) {
            m_roots[i].refresh();
            insert(new RootNode(m_cc, m_roots[i]), i);
          }
          return null;
        }
      });
      this.isExpanded = isExpanded;
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        ((RootsPanel) getComponent()).setRoots(m_roots);
        getModel().nodeStructureChanged(RootsNode.this);
        if (isExpanded) {
          m_acc.controller.expand(RootsNode.this);
        }
      }
      m_acc.controller.unblock();
      m_acc.controller.clearStatus();
    }
  }

  public void refresh() {
    boolean expanded = m_acc.controller.isExpanded(this);
    m_acc.controller.setStatus(m_acc.getMessage("dso.roots.refreshing"));
    m_acc.controller.block();
    m_acc.executorService.execute(new RefreshWorker(expanded));
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super();

      setName(AdminClient.getContext().getMessage("refresh.name"));
      setSmallIcon(RootsHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      refresh();
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

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (haveRoot(rootObjectName)) return;

          m_acc.setStatus(m_acc.getMessage("dso.root.retrieving"));

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

          m_acc.setStatus(m_acc.getMessage("dso.root.new") + root);
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

    if(m_rootsPanel != null) {
      m_rootsPanel.tearDown();
      m_rootsPanel = null;
    }
    
    m_acc = null;
    m_clusterNode = null;
    m_cc = null;
    m_roots = null;
    m_popupMenu = null;
    m_refreshAction = null;

    super.tearDown();
  }
}
