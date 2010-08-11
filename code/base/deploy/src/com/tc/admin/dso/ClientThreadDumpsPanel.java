/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractThreadDumpsPanel;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.model.IClient;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * NO LONGER USED - please keep until it's clear I'm no longer needed
 */

public class ClientThreadDumpsPanel extends AbstractThreadDumpsPanel {
  private IClient client;

  public ClientThreadDumpsPanel(ApplicationContext appContext, IClient client) {
    super(appContext);
    this.client = client;
  }

  @Override
  protected Future<String> getThreadDumpText() throws Exception {
    return appContext.submitTask(new Callable<String>() {
      public String call() throws Exception {
        return client != null && client.isReady() ? client.takeThreadDump(System.currentTimeMillis()) : "";
      }
    });
  }

  @Override
  protected Future<String> getClusterDump() throws Exception {
    return appContext.submitTask(new Callable<String>() {
      public String call() throws Exception {
        return client != null && client.isReady() ? client.takeClusterDump() : "";
      }
    });
  }

  @Override
  protected String getNodeName() {
    return client.toString();
  }
  
  @Override
  public void tearDown() {
    super.tearDown();
    client = null;
  }
}
