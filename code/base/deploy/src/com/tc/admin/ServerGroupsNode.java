/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServerGroup;

import java.awt.Component;

public class ServerGroupsNode extends ComponentNode {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected IServerGroup[]      serverGroups;
  protected ServerGroupsPanel   serverGroupsPanel;

  public ServerGroupsNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super();
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    serverGroups = clusterModel.getServerGroups();
    for (IServerGroup serverGroup : serverGroups) {
      ServerGroupNode groupNode = adminClientContext.getNodeFactory().createServerGroupNode(adminClientContext,
                                                                                            clusterModel, serverGroup);
      add(groupNode);
    }
    setLabel(adminClientContext.getMessage("server-groups") + " (" + getChildCount() + ")");

  }

  public Component getComponent() {
    if (serverGroupsPanel == null) {
      serverGroupsPanel = createServerGroupsPanel();
    }
    return serverGroupsPanel;
  }

  protected ServerGroupsPanel createServerGroupsPanel() {
    return new ServerGroupsPanel(adminClientContext, clusterModel, serverGroups);
  }

  public void tearDown() {
    super.tearDown();
    adminClientContext = null;
    clusterModel = null;
    serverGroups = null;
    serverGroupsPanel = null;
  }
}
