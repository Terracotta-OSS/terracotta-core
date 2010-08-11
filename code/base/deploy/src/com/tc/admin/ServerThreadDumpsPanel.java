/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.model.IServer;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * NO LONGER USED - please keep until it's clear I'm no longer needed
 */

public class ServerThreadDumpsPanel extends AbstractThreadDumpsPanel {
  private IServer server;

  public ServerThreadDumpsPanel(ApplicationContext appContext, IServer server) {
    super(appContext);
    this.server = server;
  }

  @Override
  protected Future<String> getThreadDumpText() throws Exception {
    return appContext.submitTask(new Callable<String>() {
      public String call() throws Exception {
        return server != null ? server.takeThreadDump(System.currentTimeMillis()) : "";
      }
    });
  }

  @Override
  protected Future<String> getClusterDump() throws Exception {
    return appContext.submitTask(new Callable<String>() {
      public String call() throws Exception {
        return server != null ? server.takeClusterDump() : "";
      }
    });
  }

  @Override
  protected String getNodeName() {
    return server.toString();
  }

  @Override
  public void tearDown() {
    super.tearDown();
    server = null;
  }
}
