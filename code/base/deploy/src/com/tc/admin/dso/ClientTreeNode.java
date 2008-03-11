/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;

public class ClientTreeNode extends ComponentNode {
  private DSOClient m_client;

  public ClientTreeNode(ConnectionContext cc, DSOClient client) {
    super(client.getRemoteAddress());
    setComponent(createClientPanel(m_client = client));
  }

  protected ClientPanel createClientPanel(DSOClient client) {
    return new ClientPanel(client);
  }
  
  public DSOClient getClient() {
    return m_client;
  }

  public void tearDown() {
    super.tearDown();
    m_client = null;
  }
}
