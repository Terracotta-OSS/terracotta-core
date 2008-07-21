/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractThreadDumpsPanel;
import com.tc.admin.model.IClient;

public class ClientThreadDumpsPanel extends AbstractThreadDumpsPanel {
  private ClientThreadDumpsNode m_clientThreadDumpsNode;

  public ClientThreadDumpsPanel(ClientThreadDumpsNode clientThreadDumpsNode) {
    super();
    m_clientThreadDumpsNode = clientThreadDumpsNode;
  }

  protected String getThreadDumpText() throws Exception {
    if(m_clientThreadDumpsNode != null) {
      IClient client = m_clientThreadDumpsNode.getClient();
      if(client != null) {
        return client.takeThreadDump(System.currentTimeMillis());
      }
    }
    return "";
  }

  public void tearDown() {
    super.tearDown();
    m_clientThreadDumpsNode = null;
  }
}
