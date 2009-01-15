/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

public class GCStatsNode extends ComponentNode {
  private ApplicationContext appContext;
  protected IClusterModel    clusterModel;
  protected GCStatsPanel     gcStatsPanel;

  public GCStatsNode(ApplicationContext appContext, IClusterModel clusterModel) {
    super();
    this.appContext = appContext;
    this.clusterModel = clusterModel;
    setLabel(appContext.getMessage("dso.gcstats"));
    setIcon(DSOHelper.getHelper().getGCIcon());
  }

  IClusterModel getClusterModel() {
    return clusterModel;
  }

  protected GCStatsPanel createGCStatsPanel() {
    return new GCStatsPanel(appContext, clusterModel);
  }

  public Component getComponent() {
    if (gcStatsPanel == null) {
      gcStatsPanel = createGCStatsPanel();
    }
    return gcStatsPanel;
  }

  public void tearDown() {
    if (gcStatsPanel != null) {
      gcStatsPanel.tearDown();
      gcStatsPanel = null;
    }
    super.tearDown();
  }
}
