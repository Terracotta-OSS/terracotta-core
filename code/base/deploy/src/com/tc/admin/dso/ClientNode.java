/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ClusterElementNode;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.model.IClient;

import java.awt.Component;

public class ClientNode extends ClusterElementNode {
  protected IAdminClientContext adminClientContext;
  protected IClient             client;
  protected ClientPanel         clientPanel;

  public ClientNode(IAdminClientContext adminClientContext, IClient client) {
    super(client);

    setLabel(client.getRemoteAddress());

    this.adminClientContext = adminClientContext;
    this.client = client;
  }

  protected ClientPanel createClientPanel() {
    return new ClientPanel(adminClientContext, client);
  }

  @Override
  public Component getComponent() {
    if (clientPanel == null) {
      clientPanel = createClientPanel();
    }
    return clientPanel;
  }

  public IClient getClient() {
    return client;
  }

  @Override
  public void tearDown() {
    synchronized (this) {
      adminClientContext = null;
      client = null;
      if (clientPanel != null) {
        clientPanel.tearDown();
        clientPanel = null;
      }
    }
    super.tearDown();
  }
}
