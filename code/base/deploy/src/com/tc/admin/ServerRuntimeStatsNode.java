/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;


import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IServer;

import java.awt.Component;

public class ServerRuntimeStatsNode extends ComponentNode {
  protected ServerNode              m_serverNode;
  protected ServerRuntimeStatsPanel m_runtimeStatsPanel;

  public ServerRuntimeStatsNode(ServerNode serverNode) {
    super("Runtime statistics");
    m_serverNode = serverNode;
    setIcon(ServerHelper.getHelper().getRuntimeStatsIcon());
  }

  IServer getServer() {
    return m_serverNode.getServer();
  }

  protected ServerRuntimeStatsPanel createRuntimeStatsPanel() {
    return new ServerRuntimeStatsPanel(this);
  }

  public Component getComponent() {
    if (m_runtimeStatsPanel == null) {
      m_runtimeStatsPanel = createRuntimeStatsPanel();
    }
    return m_runtimeStatsPanel;
  }

  public void tearDown() {
    if (m_runtimeStatsPanel != null) {
      m_runtimeStatsPanel.tearDown();
      m_runtimeStatsPanel = null;
    }
    super.tearDown();
  }
}
