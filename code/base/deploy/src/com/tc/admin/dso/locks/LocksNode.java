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
import com.tc.admin.model.IClusterModel;

import javax.swing.Icon;

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
  }

  IClusterModel getClusterModel() {
    return m_clusterNode != null ? m_clusterNode.getClusterModel() : null;
  }
  
  ConnectionContext getConnectionContext() {
    return m_clusterNode.getConnectionContext();
  }

  public boolean isProfiling() {
    return m_locksPanel != null ? m_locksPanel.isProfiling() : false;
  }

  public Icon getIcon() {
    return LocksHelper.getHelper().getLocksIcon();
  }

  public String getBaseLabel() {
    return m_baseLabel;
  }

  void showProfiling(boolean profiling) {
    setLabel(m_baseLabel + (profiling ? m_profilingSuffix : ""));
    notifyChanged();
    m_clusterNode.showProfilingLocks(profiling);
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
