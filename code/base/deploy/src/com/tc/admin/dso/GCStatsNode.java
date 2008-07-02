/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ClusterNode;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

public class GCStatsNode extends ComponentNode {
  protected ClusterNode  m_clusterNode;
  protected GCStatsPanel m_gcStatsPanel;

  public GCStatsNode(ClusterNode clusterNode) {
    super();
    m_clusterNode = clusterNode;
    setLabel(AdminClient.getContext().getMessage("dso.gcstats"));
    setIcon(DSOHelper.getHelper().getGCIcon());
  }

  IClusterModel getClusterModel() {
    return m_clusterNode.getClusterModel();
  }

  protected GCStatsPanel createGCStatsPanel() {
    return new GCStatsPanel(this);
  }

  public Component getComponent() {
    if (m_gcStatsPanel == null) {
      m_gcStatsPanel = createGCStatsPanel();
    }
    return m_gcStatsPanel;
  }

  public void tearDown() {
    if (m_gcStatsPanel != null) {
      m_gcStatsPanel.tearDown();
      m_gcStatsPanel = null;
    }
    super.tearDown();
  }
}
