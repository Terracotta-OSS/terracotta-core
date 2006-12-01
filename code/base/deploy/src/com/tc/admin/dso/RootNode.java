/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Component;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

public class RootNode extends ComponentNode {
  private ConnectionContext m_cc;
  private DSORoot           m_root;
  private RootsPanel        m_rootsPanel;
  private MoreAction        m_moreAction;
  private LessAction        m_lessAction;
  private JPopupMenu        m_popupMenu;
  private int               m_batchSize;
  private RefreshAction     m_refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";

  public RootNode(ConnectionContext cc, DSORoot root) {
    super();

    m_cc        = cc;
    m_root      = root;
    m_batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;

    initMenu();

    setLabel(root.toString());
  }

  public Component getComponent() {
    if(m_rootsPanel == null) {
      m_rootsPanel = new RootsPanel(m_cc, new DSORoot[] {m_root});
      m_rootsPanel.setNode(this);
    }

    return m_rootsPanel;
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

  public Icon getIcon() {
    return RootsHelper.getHelper().getRootIcon();
  }

  private void refresh() {
    ((RootsPanel)getComponent()).refresh();
    setLabel(m_root.toString());
    nodeChanged();
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

    m_popupMenu = null;
    m_moreAction = null;
    m_lessAction = null;
  }
}
