/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClient;

import java.awt.Component;

/**
 * NO LONGER USED - please keep until it's clear I'm no longer needed
 */

public class ClientThreadDumpsNode extends ComponentNode {
  private ApplicationContext       appContext;
  protected ClientNode             clientNode;
  protected ClientThreadDumpsPanel threadDumpsPanel;

  public ClientThreadDumpsNode(ApplicationContext appContext, ClientNode serverNode) {
    super(appContext.getString("client.thread.dumps"));
    this.appContext = appContext;
    clientNode = serverNode;
    setIcon(ClientsHelper.getHelper().getThreadDumpsIcon());
  }

  protected ClientThreadDumpsPanel createThreadDumpsPanel() {
    return new ClientThreadDumpsPanel(appContext, getClient());
  }

  IClient getClient() {
    return clientNode.getClient();
  }

  public Component getComponent() {
    if (threadDumpsPanel == null) {
      threadDumpsPanel = createThreadDumpsPanel();
    }
    return threadDumpsPanel;
  }

  public void tearDown() {
    if (threadDumpsPanel != null) {
      threadDumpsPanel.tearDown();
      threadDumpsPanel = null;
    }
    super.tearDown();
  }
}
