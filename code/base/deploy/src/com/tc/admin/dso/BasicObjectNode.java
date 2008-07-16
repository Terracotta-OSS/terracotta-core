/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IMapEntry;
import com.tc.admin.model.IObject;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class BasicObjectNode extends XTreeNode implements DSOObjectTreeNode {
  protected IBasicObject      m_object;
  private boolean             m_resident;
  private MoreAction          m_moreAction;
  private LessAction          m_lessAction;
  private JPopupMenu          m_popupMenu;
  private int                 m_batchSize;
  private RefreshAction       m_refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";

  public BasicObjectNode(IBasicObject object) {
    super(object);

    m_object = object;
    m_batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;
    setResident(true);

    init();

    if (m_object.getObjectID() != null && !m_object.isCycle()) {
      addActionBinding(REFRESH_ACTION, m_refreshAction = new RefreshAction());
    }
  }

  public IObject getObject() {
    return m_object;
  }

  public void setResident(boolean resident) {
    m_resident = resident;
  }

  public boolean isResident() {
    return m_resident;
  }

  public boolean isObjectValid() {
    return m_object.isValid();
  }

  protected void init() {
    int count = m_object.getFieldCount();

    if (children == null) {
      children = new Vector();
    }
    children.setSize(count);
  }

  private void initMenu() {
    m_popupMenu = new JPopupMenu();
    m_popupMenu.add(m_refreshAction);

    if (m_object.isArray() || m_object.isCollection()) {
      m_popupMenu.add(m_moreAction = new MoreAction());
      m_popupMenu.add(m_lessAction = new LessAction());
    }
  }

  private void testInitMenu() {
    if (m_popupMenu != null) return;
    if (m_refreshAction != null) {
      initMenu();
    }
  }

  public JPopupMenu getPopupMenu() {
    testInitMenu();
    return m_popupMenu;
  }

  private void fillInChildren() {
    int childCount = getChildCount();
    boolean nso = false;

    for (int i = 0; i < childCount; i++) {
      if (children.elementAt(i) == null) {
        IObject field = m_object.getField(i);
        XTreeNode child = createFieldNode(field);

        children.setElementAt(child, i);
        child.setParent(BasicObjectNode.this);

        if (field == null) {
          nso = true;
        }
      }
    }

    if (nso) {
      SwingUtilities.invokeLater(new AncestorReaper());
    }
  }

  class AncestorReaper implements Runnable {
    public void run() {
      XTreeNode node = BasicObjectNode.this;

      while (node != null) {
        if (node instanceof BasicObjectNode) {
          BasicObjectNode ftn = (BasicObjectNode) node;
          if (ftn.isObjectValid()) {
            ftn.refreshChildren();
            return;
          }
        }
        node = (XTreeNode) node.getParent();
      }
    }
  }

  public TreeNode getChildAt(int index) {
    if (children != null && children.elementAt(index) == null) {
      AdminClientContext acc = AdminClient.getContext();

      acc.block();
      fillInChildren();
      acc.unblock();
    }

    return super.getChildAt(index);
  }

  private XTreeNode createFieldNode(IObject object) {
    if (object instanceof IMapEntry) {
      return new MapEntryNode((IMapEntry) object);
    } else if (object instanceof IBasicObject) {
      BasicObjectTreeModel model = (BasicObjectTreeModel) getModel();
      return model.newObjectNode((IBasicObject) object);
    } else {
      return new XTreeNode("Collected...");
    }
  }

  public int getChildCount() {
    return m_object != null ? m_object.getFieldCount() : 0;
  }

  public Icon getIcon() {
    RootsHelper helper = RootsHelper.getHelper();
    return m_object.isCycle() ? helper.getCycleIcon() : helper.getFieldIcon();
  }

  public void nodeSelected(TreeSelectionEvent e) {
    if (m_object.isCycle()) {
      IObject cycleRoot = m_object.getCycleRoot();
      XTreeNode parentNode = (XTreeNode) getParent();

      while (parentNode != null) {
        if (parentNode instanceof DSOObjectTreeNode) {
          if (((DSOObjectTreeNode) parentNode).getObject() == cycleRoot) {
            JTree tree = (JTree) e.getSource();
            TreePath path = new TreePath(parentNode.getPath());
            TreePath[] paths = ((JTree) e.getSource()).getSelectionPaths();
            TreePath[] newPaths = new TreePath[paths.length + 1];

            newPaths[0] = path;
            System.arraycopy(paths, 0, newPaths, 1, paths.length);
            tree.getSelectionModel().setSelectionPaths(newPaths);
            return;
          }
        }

        parentNode = (XTreeNode) parentNode.getParent();
      }
    }
  }

  public void refreshChildren() {
    tearDownChildren();

    if (m_object != null) {
      m_object.initFields();
      children.setSize(getChildCount());
      fillInChildren();
    }

    getModel().nodeStructureChanged(this);
  }

  public void refresh() {
    AdminClientContext acc = AdminClient.getContext();
    boolean expanded = acc.isExpanded(this);
    XTreeModel model = getModel();
    XTreeNode node;

    for (int i = getChildCount() - 1; i >= 0; i--) {
      node = (XTreeNode) getChildAt(i);
      node.tearDown();
      model.removeNodeFromParent(node);
    }

    try {
      m_object.refresh();
    } catch (Exception e) {
      // TODO: ask parent to teardown
      e.printStackTrace();
    }

    init();

    model.nodeStructureChanged(BasicObjectNode.this);
    if (expanded) {
      acc.expand(this);
    }
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super("Refresh", RootsHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc = AdminClient.getContext();
      String name = m_object.getName();

      acc.setStatus("Refreshing field " + name + "...");
      acc.block();

      refresh();

      acc.clearStatus();
      acc.unblock();
    }
  }

  public void nodeClicked(MouseEvent me) {
    if (m_refreshAction != null) {
      m_refreshAction.actionPerformed(null);
    }
  }

  private class MoreAction extends XAbstractAction {
    private MoreAction() {
      super("More");
      setEnabled(m_object.isArray() || !m_object.isComplete());
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc = AdminClient.getContext();
      String name = m_object.getName();

      setEnabled(incrementDSOBatchSize() == ConnectionContext.DSO_MAX_BATCH_SIZE);
      m_lessAction.setEnabled(true);
      m_object.setBatchSize(m_batchSize);

      acc.setStatus("Refreshing " + name + "...");
      acc.block();

      refresh();

      acc.clearStatus();
      acc.unblock();
      acc.nodeChanged(BasicObjectNode.this);
    }
  }

  private class LessAction extends XAbstractAction {
    private LessAction() {
      super("Less");
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc = AdminClient.getContext();
      String name = m_object.getName();

      setEnabled(decrementDSOBatchSize() != ConnectionContext.DSO_SMALL_BATCH_SIZE);
      m_moreAction.setEnabled(true);
      m_object.setBatchSize(m_batchSize);

      acc.setStatus("Refreshing " + name + "...");
      acc.block();

      refresh();

      acc.clearStatus();
      acc.unblock();
      acc.nodeChanged(BasicObjectNode.this);
    }
  }

  int incrementDSOBatchSize() {
    switch (m_batchSize) {
      case ConnectionContext.DSO_SMALL_BATCH_SIZE:
        m_batchSize = ConnectionContext.DSO_MEDIUM_BATCH_SIZE;
        break;
      case ConnectionContext.DSO_MEDIUM_BATCH_SIZE:
        m_batchSize = ConnectionContext.DSO_LARGE_BATCH_SIZE;
        break;
      case ConnectionContext.DSO_LARGE_BATCH_SIZE:
        m_batchSize = ConnectionContext.DSO_MAX_BATCH_SIZE;
        break;
    }

    return m_batchSize;
  }

  int decrementDSOBatchSize() {
    switch (m_batchSize) {
      case ConnectionContext.DSO_MEDIUM_BATCH_SIZE:
        m_batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;
        break;
      case ConnectionContext.DSO_LARGE_BATCH_SIZE:
        m_batchSize = ConnectionContext.DSO_MEDIUM_BATCH_SIZE;
        break;
      case ConnectionContext.DSO_MAX_BATCH_SIZE:
        m_batchSize = ConnectionContext.DSO_LARGE_BATCH_SIZE;
        break;
    }

    return m_batchSize;
  }

  public int resetDSOBatchSize() {
    return m_batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;
  }

  public void tearDown() {
    super.tearDown();

    m_object = null;
    m_moreAction = null;
    m_lessAction = null;
    m_refreshAction = null;
    m_popupMenu = null;
  }
}
