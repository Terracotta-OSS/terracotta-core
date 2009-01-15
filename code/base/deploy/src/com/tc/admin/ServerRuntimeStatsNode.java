/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IServer;

import java.awt.Component;

public class ServerRuntimeStatsNode extends ComponentNode {
  protected ApplicationContext      appContext;
  protected IServer                 server;
  protected ServerRuntimeStatsPanel runtimeStatsPanel;

  public ServerRuntimeStatsNode(ApplicationContext appContext, IServer server) {
    super("Runtime statistics");
    this.appContext = appContext;
    this.server = server;
    setIcon(ServerHelper.getHelper().getRuntimeStatsIcon());
  }

  protected ServerRuntimeStatsPanel createRuntimeStatsPanel() {
    return new ServerRuntimeStatsPanel(appContext, server);
  }

  public Component getComponent() {
    if (runtimeStatsPanel == null) {
      runtimeStatsPanel = createRuntimeStatsPanel();
    }
    return runtimeStatsPanel;
  }

  public void tearDown() {
    super.tearDown();
    if (runtimeStatsPanel != null) {
      runtimeStatsPanel.tearDown();
      runtimeStatsPanel = null;
    }
    appContext = null;
    server = null;
  }
}
