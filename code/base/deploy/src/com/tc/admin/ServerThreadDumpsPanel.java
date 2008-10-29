/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IServer;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ServerThreadDumpsPanel extends AbstractThreadDumpsPanel {
  private ServerThreadDumpsNode m_serverThreadDumpsNode;

  public ServerThreadDumpsPanel(ServerThreadDumpsNode serverThreadDumpsNode) {
    super();
    m_serverThreadDumpsNode = serverThreadDumpsNode;
  }

  protected Future<String> getThreadDumpText() throws Exception {
    final IServer server = m_serverThreadDumpsNode != null ? m_serverThreadDumpsNode.getServer() : null;
    return AdminClient.getContext().submitTask(new Callable<String>() {
      public String call() throws Exception {
        return server != null ? server.takeThreadDump(System.currentTimeMillis()) : "";
      }
    });
  }

  protected String getNodeName() {
    return m_serverThreadDumpsNode.getServer().toString();
  }

  public void tearDown() {
    super.tearDown();
    m_serverThreadDumpsNode = null;
  }
}
