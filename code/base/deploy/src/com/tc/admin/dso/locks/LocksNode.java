/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.ClusterNode;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import javax.swing.Icon;

public class LocksNode extends ComponentNode {
  private final ClusterNode   clusterNode;
  private final IClusterModel clusterModel;

  private LocksPanel          locksPanel;
  private String              baseLabel;
  private final String        profilingSuffix;

  public LocksNode(IAdminClientContext adminClientContext, ClusterNode clusterNode) {
    super();

    this.clusterNode = clusterNode;
    this.clusterModel = clusterNode.getClusterModel();

    setLabel(baseLabel = adminClientContext.getString("dso.locks"));
    profilingSuffix = adminClientContext.getString("dso.locks.profiling.suffix");
    setComponent(locksPanel = new LocksPanel(adminClientContext, this));
  }

  IClusterModel getClusterModel() {
    return clusterModel;
  }

  IServer getActiveCoordinator() {
    return clusterModel.getActiveCoordinator();
  }

  public boolean isProfiling() {
    return locksPanel != null ? locksPanel.isProfiling() : false;
  }

  @Override
  public Icon getIcon() {
    return LocksHelper.getHelper().getLocksIcon();
  }

  public String getBaseLabel() {
    return baseLabel;
  }

  void showProfiling(boolean profiling) {
    setLabel(baseLabel + (profiling ? profilingSuffix : ""));
    notifyChanged();
    clusterNode.showProfilingLocks(profiling);
  }

  void notifyChanged() {
    nodeChanged();
  }
}
