/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

public class ThreadDumpsNode extends ComponentNode {
  protected final IAdminClientContext       adminClientContext;
  protected final IClusterModel             clusterModel;
  protected final ClusterThreadDumpProvider threadDumpProvider;

  protected ThreadDumpsPanel                threadDumpsPanel;

  public ThreadDumpsNode(IAdminClientContext adminClientContext, IClusterModel clusterModel,
                         ClusterThreadDumpProvider threadDumpProvider) {
    super(adminClientContext.getString("cluster.thread.dumps"));

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.threadDumpProvider = threadDumpProvider;

    setIcon(ServerHelper.getHelper().getThreadDumpsIcon());
  }

  synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public Component getComponent() {
    if (threadDumpsPanel == null) {
      threadDumpsPanel = new ThreadDumpsPanel(adminClientContext, clusterModel, threadDumpProvider);
    }
    return threadDumpsPanel;
  }

  @Override
  public void tearDown() {
    if (threadDumpsPanel != null) {
      threadDumpsPanel.tearDown();
    }
    super.tearDown();
  }
}
