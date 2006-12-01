/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ObjectNameTreeNode;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;

public class RootTreeNode extends ObjectNameTreeNode implements DSOObjectTreeNode {
  private ConnectionContext m_cc;
  private DSORoot           m_root;
  private JPopupMenu        m_popupMenu;
  private MoreAction        m_moreAction;
  private LessAction        m_lessAction;
  private int               m_batchSize;
  private RefreshAction     m_refreshAction;
  
  private static final String REFRESH_ACTION = "RefreshAction";

  public RootTreeNode(ConnectionContext cc, DSORoot root) {
    super(root.getObjectName());

    m_cc        = cc;
    m_root      = root;
    m_batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;

    initMenu();
    init();
  }

  public DSOObject getDSOObject() {
    return getDSORoot();
  }
  
  public DSORoot getDSORoot() {
    return m_root;
  }
  
  private void init() {
    int count = m_root.getFieldCount();

    if(count > 0) {
      if(children == null) {
        children = new Vector();
      }
      children.setSize(count);
    }
  }

  private void initMenu() {
    m_refreshAction = new RefreshAction();
    
    m_popupMenu = new JPopupMenu("Root Actions");
    m_popupMenu.add(m_refreshAction);
    
    if(m_root.isArray() || m_root.isCollection()) {
      m_popupMenu.add(m_moreAction = new MoreAction());
      m_popupMenu.add(m_lessAction = new LessAction());
    }

    addActionBinding(REFRESH_ACTION, m_refreshAction);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  private void fillInChildren() {
    int     childCount = getChildCount();
    boolean nso        = false;

    for(int i = 0; i < childCount; i++) {
      DSOObject field = m_root.getField(i);
      XTreeNode child = createFieldNode(field);

      children.setElementAt(child, i);
      child.setParent(this);

      if(field == null) {
        nso = true;
      }
    }

    if(nso) {
      SwingUtilities.invokeLater(new ChildReaper());      
    }
  }

  class ChildReaper implements Runnable {
    public void run() {
      refresh();
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
    return m_root != null ? m_root.getFieldCount() : 0;
  }

  public String toString() {
    return m_root.toString();
  }

  public Icon getIcon() {
    return RootsHelper.getHelper().getRootIcon();
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

    m_root.refresh();

    init();

    model.nodeStructureChanged(RootTreeNode.this);
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
      String             name = m_root.getName();

      acc.controller.setStatus("Refreshing root " + name + "...");
      acc.controller.block();

      refresh();

      acc.controller.clearStatus();
      acc.controller.unblock();
    }
  }

  public void nodeClicked(MouseEvent me) {
    m_refreshAction.actionPerformed(null);
  }
  
  private class MoreAction extends XAbstractAction {
    private MoreAction() {
      super("More");
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc  = AdminClient.getContext();
      String             name = m_root.getName();

      if(incrementDSOBatchSize() == ConnectionContext.DSO_MAX_BATCH_SIZE) {
        setEnabled(false);
      }
      m_lessAction.setEnabled(true);
      m_root.setBatchSize(m_batchSize);
      
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
      String             name = m_root.getName();

      if(decrementDSOBatchSize() == ConnectionContext.DSO_SMALL_BATCH_SIZE) {
        setEnabled(false);
      }
      m_moreAction.setEnabled(true);
      m_root.setBatchSize(m_batchSize);
      
      acc.controller.setStatus("Refreshing root " + name + "...");
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

    m_cc        = null;
    m_root      = null;
    m_popupMenu = null;
  }
}
