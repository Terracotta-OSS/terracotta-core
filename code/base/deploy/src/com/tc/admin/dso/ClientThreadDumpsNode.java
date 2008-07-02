/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClient;

import java.awt.Component;

public class ClientThreadDumpsNode extends ComponentNode {
  protected ClientNode             m_clientNode;
  protected ClientThreadDumpsPanel m_threadDumpsPanel;

  public ClientThreadDumpsNode(ClientNode serverNode) {
    super(AdminClient.getContext().getString("client.thread.dumps"));
    m_clientNode = serverNode;
    setIcon(ClientsHelper.getHelper().getThreadDumpsIcon());
  }

  protected ClientThreadDumpsPanel createThreadDumpsPanel() {
    return new ClientThreadDumpsPanel(this);
  }

  IClient getClient() {
    return m_clientNode.getClient();
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
