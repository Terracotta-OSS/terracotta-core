/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.IAdminClientContext;
import com.tc.admin.ServerHelper;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

public class RuntimeStatsNode extends ComponentNode {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected RuntimeStatsPanel   runtimeStatsPanel;

  public RuntimeStatsNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(adminClientContext.getString("dso.runtime.stats"));

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    setIcon(ServerHelper.getHelper().getRuntimeStatsIcon());
  }

  synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public Component getComponent() {
    if (runtimeStatsPanel == null) {
      runtimeStatsPanel = new RuntimeStatsPanel(adminClientContext, clusterModel);
    }
    return runtimeStatsPanel;
  }

  @Override
  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      if (runtimeStatsPanel != null) {
        runtimeStatsPanel.tearDown();
        runtimeStatsPanel = null;
      }
    }
  }
}
