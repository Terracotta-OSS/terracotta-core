/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ClusterNode;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XTreeCellRenderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;

public class LocksNode extends ComponentNode {
  private AdminClientContext m_acc;
  private ClusterNode        m_clusterNode;
  private LocksPanel         m_locksPanel;
  private String             m_baseLabel;
  private String             m_profilingSuffix;

  public LocksNode(ClusterNode clusterNode) {
    super();

    m_acc = AdminClient.getContext();
    m_clusterNode = clusterNode;
    setLabel(m_baseLabel = m_acc.getString("dso.locks"));
    m_profilingSuffix = m_acc.getString("dso.locks.profiling.suffix");
    setComponent(m_locksPanel = new LocksPanel(this));
    setRenderer(new LocksNodeRenderer());
  }

  ConnectionContext getConnectionContext() {
    return m_clusterNode.getConnectionContext();
  }

  public void newConnectionContext() {
    if (m_locksPanel != null) {
      m_locksPanel.newConnectionContext();
    }
  }

  private class LocksNodeRenderer extends XTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean focused) {
      Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
      if(m_acc == null) return comp;
      if (m_locksPanel.isProfiling()) {
        m_label.setForeground(sel ? Color.white : Color.red);
        m_label.setText(getBaseLabel() + m_profilingSuffix);
      }
      return comp;
    }
  }

  public Icon getIcon() {
    return LocksHelper.getHelper().getLocksIcon();
  }

  public String getBaseLabel() {
    return m_baseLabel;
  }

  void notifyChanged() {
    nodeChanged();
  }

  public void tearDown() {
    super.tearDown();

    m_acc = null;
    m_clusterNode = null;
    m_baseLabel = null;
    m_locksPanel = null;
  }
}
