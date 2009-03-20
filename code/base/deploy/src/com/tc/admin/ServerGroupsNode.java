/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServerGroup;

import java.awt.Component;

public class ServerGroupsNode extends ComponentNode {
  protected ApplicationContext appContext;
  protected IClusterModel      clusterModel;
  protected IServerGroup[]     serverGroups;
  protected ServerGroupsPanel  serverGroupsPanel;

  public ServerGroupsNode(ApplicationContext appContext, IClusterModel clusterModel) {
    super();
    this.appContext = appContext;
    this.clusterModel = clusterModel;
    serverGroups = clusterModel.getServerGroups();
    for (IServerGroup serverGroup : serverGroups) {
      add(new ServerGroupNode(appContext, clusterModel, serverGroup));
    }
    setLabel(appContext.getMessage("server-groups") + " (" + getChildCount() + ")");
  }

  @Override
  public Component getComponent() {
    if (serverGroupsPanel == null) {
      serverGroupsPanel = createServerGroupsPanel();
    }
    return serverGroupsPanel;
  }

  protected ServerGroupsPanel createServerGroupsPanel() {
    return new ServerGroupsPanel(appContext, clusterModel, serverGroups);
  }

  @Override
  public void tearDown() {
    super.tearDown();
    appContext = null;
    clusterModel = null;
    serverGroups = null;
    serverGroupsPanel = null;
  }
}
