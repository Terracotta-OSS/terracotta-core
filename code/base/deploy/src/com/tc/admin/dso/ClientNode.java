/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClient;

import java.awt.Component;

public class ClientNode extends ComponentNode {
  protected ClientsNode            m_clientsNode;
  protected IClient                m_client;
  protected ClientPanel            m_clientPanel;
  protected ClientThreadDumpsNode  m_threadDumpsNode;
  protected ClientRuntimeStatsNode m_runtimeStatsNode;
  protected ClientRootsNode        m_rootsNode;

  public ClientNode(ClientsNode clientsNode, IClient client) {
    super(client.getRemoteAddress());
    m_clientsNode = clientsNode;
    m_client = client;
    addChildren();
  }

  ClientsNode getClientsNode() {
    return m_clientsNode;
  }
  
  protected ClientPanel createClientPanel() {
    return new ClientPanel(this);
  }

  public Component getComponent() {
    if (m_clientPanel == null) {
      m_clientPanel = createClientPanel();
    }
    return m_clientPanel;
  }

  protected void addChildren() {
    add(m_rootsNode = createClientRootsNode());
    add(m_runtimeStatsNode = createRuntimeStatsNode());
    add(m_threadDumpsNode = createThreadDumpsNode());
  }

  protected ClientRuntimeStatsNode createRuntimeStatsNode() {
    return new ClientRuntimeStatsNode(this);
  }

  protected ClientThreadDumpsNode createThreadDumpsNode() {
    return new ClientThreadDumpsNode(this);
  }

  protected ClientRootsNode createClientRootsNode() {
    return new ClientRootsNode(this);
  }
  
  public IClient getClient() {
    return m_client;
  }

  public void tearDown() {
    if (m_clientPanel != null) {
      m_clientPanel.tearDown();
      m_clientPanel = null;
    }

    super.tearDown();

    m_clientsNode = null;
    m_client = null;
    m_rootsNode = null;
    m_threadDumpsNode = null;
    m_runtimeStatsNode = null;
  }
}
