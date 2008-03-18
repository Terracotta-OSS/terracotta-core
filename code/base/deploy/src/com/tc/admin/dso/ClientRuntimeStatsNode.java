/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Component;

import com.tc.admin.common.ComponentNode;
import com.tc.management.beans.l1.L1InfoMBean;

public class ClientRuntimeStatsNode extends ComponentNode {
  private ClientNode                m_clientNode;
  protected ClientRuntimeStatsPanel m_runtimeStatsPanel;

  public ClientRuntimeStatsNode(ClientNode serverNode) {
    super("Runtime statistics");
    m_clientNode = serverNode;
    setIcon(ClientsHelper.getHelper().getRuntimeStatsIcon());
  }

  protected ClientRuntimeStatsPanel createRuntimeStatsPanel() {
    return new ClientRuntimeStatsPanel(this);
  }

  public Component getComponent() {
    if (m_runtimeStatsPanel == null) {
      m_runtimeStatsPanel = createRuntimeStatsPanel();
    }
    return m_runtimeStatsPanel;
  }

  L1InfoMBean getL1InfoBean() throws Exception {
    return m_clientNode.getL1InfoBean();
  }

  DSOClient getClient() {
    return m_clientNode.getClient();
  }

  public void tearDown() {
    if (m_runtimeStatsPanel != null) {
      m_runtimeStatsPanel.tearDown();
      m_runtimeStatsPanel = null;
    }
    super.tearDown();
  }
}
