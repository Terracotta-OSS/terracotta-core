/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IServer;

import java.awt.Component;

public class ServerThreadDumpsNode extends ComponentNode {
  protected ServerNode             m_serverNode;
  protected ServerThreadDumpsPanel m_threadDumpsPanel;

  public ServerThreadDumpsNode(ServerNode serverNode) {
    super(AdminClient.getContext().getString("server.thread.dumps"));
    m_serverNode = serverNode;
    setIcon(ServerHelper.getHelper().getThreadDumpsIcon());
  }

  IServer getServer() {
    return m_serverNode.getServer();
  }

  protected ServerThreadDumpsPanel createThreadDumpsPanel() {
    return new ServerThreadDumpsPanel(this);
  }

  public Component getComponent() {
    if (m_threadDumpsPanel == null) {
      m_threadDumpsPanel = createThreadDumpsPanel();
    }
    return m_threadDumpsPanel;
  }

  public void tearDown() {
    if (m_threadDumpsPanel != null) {
      m_threadDumpsPanel.tearDown();
      m_threadDumpsPanel = null;
    }
    super.tearDown();
  }
}
