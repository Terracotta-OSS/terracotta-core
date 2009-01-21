/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IServer;

import java.awt.Component;

/**
 * NO LONGER USED - please keep until it's clear I'm no longer needed
 */

public class ServerThreadDumpsNode extends ComponentNode {
  private ApplicationContext       appContext;
  protected IServer                server;
  protected ServerThreadDumpsPanel threadDumpsPanel;

  public ServerThreadDumpsNode(ApplicationContext appContext, IServer server) {
    super();
    
    this.appContext = appContext;
    this.server = server;

    setUserObject(appContext.getString("server.thread.dumps"));
    setIcon(ServerHelper.getHelper().getThreadDumpsIcon());
  }

  IServer getServer() {
    return server;
  }

  protected ServerThreadDumpsPanel createThreadDumpsPanel() {
    return new ServerThreadDumpsPanel(appContext, server);
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
