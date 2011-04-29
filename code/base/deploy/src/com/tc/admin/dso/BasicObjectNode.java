/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.IAdminClientContext;
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
  protected final IAdminClientContext adminClientContext;
  protected final IBasicObject        object;

  private boolean                     resident;
  private MoreAction                  moreAction;
  private LessAction                  lessAction;
  private JPopupMenu                  popupMenu;
  private int                         batchSize;
  private RefreshAction               refreshAction;

  private static final String         REFRESH_ACTION = "RefreshAction";

  public BasicObjectNode(IAdminClientContext adminClientContext, IBasicObject object) {
    super(object);

    this.adminClientContext = adminClientContext;
    this.object = object;
    batchSize = object.getBatchSize();
    setResident(true);

    init();

    if (object.getObjectID() != null && !object.isCycle()) {
      addActionBinding(REFRESH_ACTION, refreshAction = new RefreshAction());
    }
  }

  public IObject getObject() {
    return object;
  }

  public void setResident(boolean resident) {
    this.resident = resident;
  }

  public boolean isResident() {
    return resident;
  }

  public boolean isObjectValid() {
    return object.isValid();
  }

  protected void init() {
    int count = getChildCount();
    if (children == null) {
      children = new Vector();
    }
    children.setSize(count);
  }

  private void initMenu() {
    popupMenu = new JPopupMenu();
    popupMenu.add(refreshAction);

    if (object.isArray() || object.isCollection()) {
      popupMenu.add(moreAction = new MoreAction());
      popupMenu.add(lessAction = new LessAction());
    }
  }

  private void testInitMenu() {
    if (popupMenu != null) return;
    if (refreshAction != null) {
      initMenu();
    }
  }

  @Override
  public JPopupMenu getPopupMenu() {
    testInitMenu();
    return popupMenu;
  }

  private void fillInChildren() {
    int childCount = children.size();
    boolean nso = false;

    for (int i = 0; i < childCount; i++) {
      if (children.elementAt(i) == null) {
        IObject field = object.getField(i);
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

  @Override
  public TreeNode getChildAt(int index) {
    if (children != null && children.elementAt(index) == null) {
      fillInChildren();
    }
    return super.getChildAt(index);
  }

  private XTreeNode createFieldNode(IObject theObject) {
    if (theObject instanceof IMapEntry) {
      return new MapEntryNode((IMapEntry) theObject);
    } else if (theObject instanceof IBasicObject) {
      BasicObjectTreeModel model = (BasicObjectTreeModel) getModel();
      return model.newObjectNode((IBasicObject) theObject);
    } else {
      return new XTreeNode("Collected.");
    }
  }

  @Override
  public int getChildCount() {
    return object.getFieldCount();
  }

  @Override
  public Icon getIcon() {
    RootsHelper helper = RootsHelper.getHelper();
    return object.isCycle() ? helper.getCycleIcon() : helper.getFieldIcon();
  }

  @Override
  public void nodeSelected(TreeSelectionEvent e) {
    if (object.isCycle()) {
      IObject cycleRoot = object.getCycleRoot();
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

    object.initFields();
    children.setSize(getChildCount());
    fillInChildren();

    getModel().nodeStructureChanged(this);
  }

  public void refresh() {
    boolean expanded = adminClientContext.getAdminClientController().isExpanded(this);
    XTreeModel model = getModel();
    XTreeNode node;

    for (int i = children.size() - 1; i >= 0; i--) {
      node = (XTreeNode) getChildAt(i);
      if (node != null) {
        model.removeNodeFromParent(node);
      }
    }

    try {
      object.refresh();
    } catch (Exception e) {
      // TODO: ask parent to tearDown
      e.printStackTrace();
    }

    init();

    model.nodeStructureChanged(BasicObjectNode.this);
    if (expanded) {
      adminClientContext.getAdminClientController().expand(this);
    }
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super(adminClientContext.getString("refresh"), RootsHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      adminClientContext.setStatus(adminClientContext.format("refreshing.field.pattern", object.getName()));
      adminClientContext.block();

      refresh();

      adminClientContext.clearStatus();
      adminClientContext.unblock();
    }
  }

  @Override
  public void nodeClicked(MouseEvent me) {
    if (refreshAction != null) {
      refreshAction.actionPerformed(null);
    }
  }

  private class MoreAction extends XAbstractAction {
    private MoreAction() {
      super(adminClientContext.getString("more"));
      setEnabled(batchSize == ConnectionContext.DSO_SMALL_BATCH_SIZE);
    }

    public void actionPerformed(ActionEvent ae) {
      setEnabled(incrementDSOBatchSize() != ConnectionContext.DSO_MAX_BATCH_SIZE);
      lessAction.setEnabled(true);
      object.setBatchSize(batchSize);

      adminClientContext.setStatus(adminClientContext.format("refreshing.field.pattern", object.getName()));
      adminClientContext.block();

      refresh();

      adminClientContext.clearStatus();
      adminClientContext.unblock();
      nodeChanged();
    }
  }

  private class LessAction extends XAbstractAction {
    private LessAction() {
      super(adminClientContext.getString("less"));
      setEnabled(batchSize > ConnectionContext.DSO_SMALL_BATCH_SIZE);
    }

    public void actionPerformed(ActionEvent ae) {
      setEnabled(decrementDSOBatchSize() != ConnectionContext.DSO_SMALL_BATCH_SIZE);
      moreAction.setEnabled(true);
      object.setBatchSize(batchSize);

      adminClientContext.setStatus(adminClientContext.format("refreshing.field.pattern", object.getName()));
      adminClientContext.block();

      refresh();

      adminClientContext.clearStatus();
      adminClientContext.unblock();
      nodeChanged();
    }
  }

  int incrementDSOBatchSize() {
    switch (batchSize) {
      case ConnectionContext.DSO_SMALL_BATCH_SIZE:
        batchSize = ConnectionContext.DSO_MEDIUM_BATCH_SIZE;
        break;
      case ConnectionContext.DSO_MEDIUM_BATCH_SIZE:
        batchSize = ConnectionContext.DSO_LARGE_BATCH_SIZE;
        break;
      case ConnectionContext.DSO_LARGE_BATCH_SIZE:
        batchSize = ConnectionContext.DSO_MAX_BATCH_SIZE;
        break;
    }

    return batchSize;
  }

  int decrementDSOBatchSize() {
    switch (batchSize) {
      case ConnectionContext.DSO_MEDIUM_BATCH_SIZE:
        batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;
        break;
      case ConnectionContext.DSO_LARGE_BATCH_SIZE:
        batchSize = ConnectionContext.DSO_MEDIUM_BATCH_SIZE;
        break;
      case ConnectionContext.DSO_MAX_BATCH_SIZE:
        batchSize = ConnectionContext.DSO_LARGE_BATCH_SIZE;
        break;
    }

    return batchSize;
  }

  public int resetDSOBatchSize() {
    return batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;
  }
}
