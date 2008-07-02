/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;


import com.tc.admin.common.ComponentNode;

import java.awt.Component;

public class ClusterThreadDumpsNode extends ComponentNode {
  protected ClusterNode m_clusterNode;
  protected ClusterThreadDumpsPanel m_clusterThreadDumpsPanel;
  
  public ClusterThreadDumpsNode(ClusterNode clusterNode) {
    super();
    setLabel(AdminClient.getContext().getString("cluster.thread.dumps"));
    m_clusterNode = clusterNode;
    setIcon(ServerHelper.getHelper().getThreadDumpsIcon());
  }
  
  protected ClusterThreadDumpsPanel createClusterThreadDumpsPanel() {
    return new ClusterThreadDumpsPanel(this);
  }
  
  public Component getComponent() {
    if(m_clusterThreadDumpsPanel == null) {
      m_clusterThreadDumpsPanel = createClusterThreadDumpsPanel();
    }
    return m_clusterThreadDumpsPanel;
  }
  
  ClusterThreadDumpEntry takeThreadDump() {
    return m_clusterNode.takeThreadDump();
  }
  
  public void tearDown() {
    if(m_clusterThreadDumpsPanel != null) {
      m_clusterThreadDumpsPanel.tearDown();
      m_clusterThreadDumpsPanel = null;
    }
    super.tearDown();
  }
}
