/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

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

public class FieldTreeNode extends XTreeNode implements DSOObjectTreeNode {
  protected ConnectionContext m_cc;
  protected DSOField          m_field;
  private MoreAction          m_moreAction;
  private LessAction          m_lessAction;
  private JPopupMenu          m_popupMenu;
  private int                 m_batchSize;
  private RefreshAction       m_refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";
  
  public FieldTreeNode(ConnectionContext cc, DSOField field) {
    super(field);

    m_cc        = cc;
    m_field     = field;
    m_batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;

    if(!field.isPrimitive()) {
      init();
      initMenu();
    }
  }

  public DSOObject getDSOObject() {
    return getField();
  }
  
  protected void init() {
    int count = m_field.getFieldCount();

    if(children == null) {
      children = new Vector();
    }
    children.setSize(count);
  }
  
  private void initMenu() {
    m_refreshAction = new RefreshAction();

    m_popupMenu = new JPopupMenu("Root Actions");
    m_popupMenu.add(m_refreshAction);
    
    if(m_field.isArray() || m_field.isCollection()) {
      m_popupMenu.add(m_moreAction = new MoreAction());
      m_popupMenu.add(m_lessAction = new LessAction());
    }

    addActionBinding(REFRESH_ACTION, m_refreshAction);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  public DSOField getField() {
    return m_field;
  }

  private void fillInChildren() {
    int     childCount = getChildCount();
    boolean nso        = false;

    for(int i = 0; i < childCount; i++) {
      if(children.elementAt(i) == null) {
        DSOObject field = m_field.getField(i);
        XTreeNode child = createFieldNode(field);

        children.setElementAt(child, i);
        child.setParent(FieldTreeNode.this);

        if(field == null) {
          nso = true;
        }
      }
    }

    if(nso) {
      SwingUtilities.invokeLater(new AncestorReaper());
    }
  }

  class AncestorReaper implements Runnable {
    public void run() {
      XTreeNode node = FieldTreeNode.this;

      while(node != null) {
        if(node instanceof FieldTreeNode) {
          FieldTreeNode ftn = (FieldTreeNode)node;

          if(ftn.getField().isValid()) {
            ftn.refreshChildren();
            return;
          }
        }
        else if(node instanceof RootTreeNode) {
          ((RootTreeNode)node).refresh();
          return;
        }

        node = (XTreeNode)node.getParent();
      }
    }
  }

  public TreeNode getChildAt(int index) {
    if(children != null && children.elementAt(index) == null) {
      AdminClientContext acc = AdminClient.getContext();

      acc.controller.block();
      fillInChildren();
      acc.controller.unblock();
    }

    return super.getChildAt(index);
  }

  private XTreeNode createFieldNode(DSOObject field) {
    return RootsHelper.getHelper().createFieldNode(m_cc, field);
  }

  public int getChildCount() {
    return m_field != null ? m_field.getFieldCount() : 0;
  }

  public Icon getIcon() {
    RootsHelper helper = RootsHelper.getHelper();
    
    return m_field.isCycle() ? helper.getCycleIcon() : helper.getFieldIcon();
  }

  public void nodeSelected(TreeSelectionEvent e) {
    if(m_field.isCycle()) {
      DSOObject cycleRoot  = m_field.getCycleRoot();
      XTreeNode parentNode = (XTreeNode)getParent();

      while(parentNode != null) {
        if(parentNode instanceof DSOObjectTreeNode) {
          if(((DSOObjectTreeNode)parentNode).getDSOObject() == cycleRoot) {
            JTree      tree     = (JTree)e.getSource();
            TreePath   path     = new TreePath(parentNode.getPath());
            TreePath[] paths    = ((JTree)e.getSource()).getSelectionPaths();
            TreePath[] newPaths = new TreePath[paths.length+1];
            
            newPaths[0] = path;
            System.arraycopy(paths, 0, newPaths, 1, paths.length);
            tree.getSelectionModel().setSelectionPaths(newPaths);
            return;
          }
        }
        
        parentNode = (XTreeNode)parentNode.getParent();
      }
    }
  }

  public void refreshChildren() {
    tearDownChildren();

    if(m_field != null) {
      m_field.initFields();
      children.setSize(getChildCount());
      fillInChildren();
    }

    getModel().nodeStructureChanged(this);
  }

  public void refresh() {
    AdminClientContext acc      = AdminClient.getContext();
    boolean            expanded = acc.controller.isExpanded(this);
    XTreeModel         model    = getModel();
    XTreeNode          node;

    for(int i = getChildCount()-1; i >= 0; i--) {
      node = (XTreeNode)getChildAt(i);
      node.tearDown();
      model.removeNodeFromParent(node);
    }

    try {
      m_field.refresh();
    } catch(Exception e) {
      // TODO: ask parent to teardown
      e.printStackTrace();
    }

    init();

    model.nodeStructureChanged(FieldTreeNode.this);
    if(expanded) {
      acc.controller.expand(this);
    }
  }
  
  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super("Refresh", RootsHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc  = AdminClient.getContext();
      String             name = m_field.getName();

      acc.controller.setStatus("Refreshing field " + name + "...");
      acc.controller.block();

      refresh();

      acc.controller.clearStatus();
      acc.controller.unblock();
    }
  }

  public void nodeClicked(MouseEvent me) {
    if(m_refreshAction != null) {
      m_refreshAction.actionPerformed(null);
    }
  }

  private class MoreAction extends XAbstractAction {
    private MoreAction() {
      super("More");
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc  = AdminClient.getContext();
      String             name = m_field.getName();

      if(incrementDSOBatchSize() == ConnectionContext.DSO_MAX_BATCH_SIZE) {
        setEnabled(false);
      }
      m_lessAction.setEnabled(true);
      m_field.setBatchSize(m_batchSize);
      
      acc.controller.setStatus("Refreshing root " + name + "...");
      acc.controller.block();

      refresh();

      acc.controller.clearStatus();
      acc.controller.unblock();
    }
  }
  
  private class LessAction extends XAbstractAction {
    private LessAction() {
      super("Less");
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc  = AdminClient.getContext();
      String             name = m_field.getName();

      if(decrementDSOBatchSize() == ConnectionContext.DSO_SMALL_BATCH_SIZE) {
        setEnabled(false);
      }
      m_moreAction.setEnabled(true);
      m_field.setBatchSize(m_batchSize);
      
      acc.controller.setStatus("Refreshing field " + name + "...");
      acc.controller.block();

      refresh();

      acc.controller.clearStatus();
      acc.controller.unblock();
    }
  }
  
  int incrementDSOBatchSize() {
    switch(m_batchSize) {
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
    switch(m_batchSize) {
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

    m_cc    = null;
    m_field = null;
  }
}
