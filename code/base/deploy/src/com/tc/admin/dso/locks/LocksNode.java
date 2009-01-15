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
  private ClusterNode clusterNode;
  private LocksPanel  locksPanel;
  private String      baseLabel;
  private String      profilingSuffix;

  public LocksNode(IAdminClientContext adminClientContext, ClusterNode clusterNode) {
    super();

    this.clusterNode = clusterNode;
    setLabel(baseLabel = adminClientContext.getString("dso.locks"));
    profilingSuffix = adminClientContext.getString("dso.locks.profiling.suffix");
    setComponent(locksPanel = new LocksPanel(adminClientContext, this));
  }

  IClusterModel getClusterModel() {
    return clusterNode != null ? clusterNode.getClusterModel() : null;
  }

  IServer getActiveCoordinator() {
    IClusterModel clusterModel = getClusterModel();
    return clusterModel != null ? clusterModel.getActiveCoordinator() : null;
  }
  
  public boolean isProfiling() {
    return locksPanel != null ? locksPanel.isProfiling() : false;
  }

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

  public void tearDown() {
    super.tearDown();

    clusterNode = null;
    baseLabel = null;
    locksPanel = null;
  }
}
