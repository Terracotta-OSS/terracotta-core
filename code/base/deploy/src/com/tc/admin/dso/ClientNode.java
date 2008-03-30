/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Component;

import com.tc.admin.common.ComponentNode;
import com.tc.management.beans.l1.L1InfoMBean;

public class ClientNode extends ComponentNode {
  protected ClientsNode            m_clientsNode;
  protected DSOClient              m_client;
  protected ClientPanel            m_clientPanel;
  protected ClientThreadDumpsNode  m_threadDumpsNode;
  protected ClientRuntimeStatsNode m_runtimeStatsNode;

  public ClientNode(ClientsNode clientsNode, DSOClient client) {
    super(client.getRemoteAddress());
    m_clientsNode = clientsNode;
    m_client = client;
    addChildren();
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
    add(m_runtimeStatsNode = createRuntimeStatsNode());
    add(m_threadDumpsNode = createThreadDumpsNode());
  }

  protected ClientRuntimeStatsNode createRuntimeStatsNode() {
    return new ClientRuntimeStatsNode(this);
  }

  protected ClientThreadDumpsNode createThreadDumpsNode() {
    return new ClientThreadDumpsNode(this);
  }

  public DSOClient getClient() {
    return m_client;
  }

  L1InfoMBean getL1InfoBean() throws Exception {
    return m_client.getL1InfoMBean();
  }

  public void tearDown() {
    if (m_clientPanel != null) {
      m_clientPanel.tearDown();
      m_clientPanel = null;
    }

    super.tearDown();

    m_clientsNode = null;
    m_client = null;
    m_threadDumpsNode = null;
    m_runtimeStatsNode = null;
  }
}
