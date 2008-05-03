/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Component;

import com.tc.admin.AdminClient;
import com.tc.admin.common.ComponentNode;
import com.tc.management.beans.l1.L1InfoMBean;

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

  public Component getComponent() {
    if (m_threadDumpsPanel == null) {
      m_threadDumpsPanel = createThreadDumpsPanel();
    }
    return m_threadDumpsPanel;
  }

  L1InfoMBean getL1InfoBean() throws Exception {
    return m_clientNode.getL1InfoBean();
  }

  public void tearDown() {
    if (m_threadDumpsPanel != null) {
      m_threadDumpsPanel.tearDown();
      m_threadDumpsPanel = null;
    }
    super.tearDown();
  }
}
