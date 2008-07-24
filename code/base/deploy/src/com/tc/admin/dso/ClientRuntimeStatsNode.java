/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClient;

import java.awt.Component;

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
      AdminClient.getContext().block();
      m_runtimeStatsPanel = createRuntimeStatsPanel();
      AdminClient.getContext().unblock();      
    }
    return m_runtimeStatsPanel;
  }

  IClient getClient() {
    return m_clientNode.getClient();
  }

  public void tearDown() {
    super.tearDown();
    if (m_runtimeStatsPanel != null) {
      m_runtimeStatsPanel.tearDown();
      m_runtimeStatsPanel = null;
    }
    m_clientNode = null;
  }
}
