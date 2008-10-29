/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractThreadDumpsPanel;
import com.tc.admin.AdminClient;
import com.tc.admin.model.IClient;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ClientThreadDumpsPanel extends AbstractThreadDumpsPanel {
  private ClientThreadDumpsNode m_clientThreadDumpsNode;

  public ClientThreadDumpsPanel(ClientThreadDumpsNode clientThreadDumpsNode) {
    super();
    m_clientThreadDumpsNode = clientThreadDumpsNode;
  }

  protected Future<String> getThreadDumpText() throws Exception {
    final IClient client = m_clientThreadDumpsNode != null ? m_clientThreadDumpsNode.getClient() : null;
    return AdminClient.getContext().submitTask(new Callable<String>() {
      public String call() throws Exception {
        return client != null ? client.takeThreadDump(System.currentTimeMillis()) : "";
      }
    });
  }

  protected String getNodeName() {
    return m_clientThreadDumpsNode.getClient().toString();
  }
  
  public void tearDown() {
    super.tearDown();
    m_clientThreadDumpsNode = null;
  }
}
