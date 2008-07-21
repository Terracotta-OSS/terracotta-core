/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IServer;

public class ServerThreadDumpsPanel extends AbstractThreadDumpsPanel {
  private ServerThreadDumpsNode m_serverThreadDumpsNode;

  public ServerThreadDumpsPanel(ServerThreadDumpsNode serverThreadDumpsNode) {
    super();
    m_serverThreadDumpsNode = serverThreadDumpsNode;
  }

  protected String getThreadDumpText() throws Exception {
    if(m_serverThreadDumpsNode != null) {
      IServer server = m_serverThreadDumpsNode.getServer();
      if(server != null) {
        return server.takeThreadDump(System.currentTimeMillis());
      }
    }
    return "";
  }

  public void tearDown() {
    super.tearDown();
    m_serverThreadDumpsNode = null;
  }
}
