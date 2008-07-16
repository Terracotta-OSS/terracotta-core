/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ClusterNode;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.RootCreationListener;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class RootsNode extends ComponentNode implements RootCreationListener, PropertyChangeListener {
  protected AdminClientContext m_acc;
  protected ClusterNode        m_clusterNode;
  protected IBasicObject[]     m_roots;
  protected RootsPanel         m_rootsPanel;
  protected JPopupMenu         m_popupMenu;
  protected RefreshAction      m_refreshAction;

  private static final String  REFRESH_ACTION = "RefreshAction";

  public RootsNode(ClusterNode clusterNode) {
    super();
    m_acc = AdminClient.getContext();
    setLabel(m_acc.getMessage("dso.roots"));
    m_clusterNode = clusterNode;
    clusterNode.getClusterModel().addPropertyChangeListener(this);
    init();
  }

  IClusterModel getClusterModel() {
    return m_clusterNode.getClusterModel();
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModel.PROP_ACTIVE_SERVER.equals(prop)) {
      SwingUtilities.invokeLater(new InitRunnable());
    }
  }

  private class InitRunnable implements Runnable {
    public void run() {
      if (m_acc == null) return;
      init();
    }
  }

  private void init() {
    if (m_acc == null) return;
    m_roots = new IBasicObject[0];
    for (int i = getChildCount() - 1; i >= 0; i--) {
      m_acc.remove((XTreeNode) getChildAt(i));
    }
    if (m_rootsPanel != null) {
      m_rootsPanel.clearModel();
    }
    m_acc.execute(new InitWorker());
  }

  private class InitWorker extends BasicWorker<IBasicObject[]> {
    private InitWorker() {
      super(new Callable<IBasicObject[]>() {
        public IBasicObject[] call() throws Exception {
          return m_clusterNode.getClusterModel().getRoots();
        }
      });
    }

    protected void finished() {
      if (m_acc == null) return;
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        m_roots = getResult();
        initMenu();
        m_acc.nodeStructureChanged(RootsNode.this);
        if (m_rootsPanel != null) {
          m_rootsPanel.setObjects(m_roots);
        }
      }
    }
  }

  protected RootsPanel createRootsPanel() {
    return new RootsPanel(getClusterModel(), m_roots);
  }

  public Component getComponent() {
    if (m_rootsPanel == null) {
      m_rootsPanel = createRootsPanel();
      m_rootsPanel.setNode(this);
    }
    return m_rootsPanel;
  }

  public IBasicObject getRoot(int index) {
    return m_roots != null ? m_roots[index] : null;
  }

  public int getRootCount() {
    return m_roots != null ? m_roots.length : 0;
  }

  private void initMenu() {
    m_refreshAction = new RefreshAction();

    m_popupMenu = new JPopupMenu();
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
          m_roots = m_clusterNode.getClusterModel().getRoots();
          for (int i = 0; i < m_roots.length; i++) {
            m_roots[i].refresh();
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
        ((RootsPanel) getComponent()).setObjects(m_roots);
        getModel().nodeStructureChanged(RootsNode.this);
        if (isExpanded) {
          m_acc.expand(RootsNode.this);
        }
      }
      m_acc.unblock();
      m_acc.clearStatus();
    }
  }

  public void refresh() {
    m_acc.setStatus(m_acc.getMessage("dso.roots.refreshing"));
    m_acc.block();
    m_acc.execute(new RefreshWorker(m_acc.isExpanded(this)));
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

  public void tearDown() {
    m_acc = null;

    m_clusterNode.getClusterModel().removePropertyChangeListener(this);
    m_clusterNode.getClusterModel().removeRootCreationListener(this);

    if (m_rootsPanel != null) {
      m_rootsPanel.tearDown();
      m_rootsPanel = null;
    }

    m_clusterNode = null;
    m_roots = null;
    m_popupMenu = null;
    m_refreshAction = null;

    super.tearDown();
  }

  public void rootCreated(IBasicObject root) {
    SwingUtilities.invokeLater(new RootCreatedRunnable(root));
  }

  private class RootCreatedRunnable implements Runnable {
    private IBasicObject m_root;

    private RootCreatedRunnable(IBasicObject root) {
      m_root = root;
    }

    public void run() {
      m_acc.setStatus(m_acc.getMessage("dso.root.retrieving"));

      ArrayList<IBasicObject> list = new ArrayList<IBasicObject>(Arrays.asList(m_roots));
      list.add(m_root);
      m_roots = list.toArray(new IBasicObject[list.size()]);
      ((RootsPanel) getComponent()).add(m_root);

      m_acc.setStatus(m_acc.getMessage("dso.root.new") + m_root);
    }
  }
}
