/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;

import java.awt.Component;

public class ServerGroupNode extends ClusterElementNode {
  protected ApplicationContext appContext;
  protected IClusterModel      clusterModel;
  protected IServerGroup       serverGroup;
  protected ServersPanel       serverGroupPanel;

  public ServerGroupNode(ApplicationContext appContext, IClusterModel clusterModel, IServerGroup serverGroup) {
    super(serverGroup);

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.serverGroup = serverGroup;

    for (IServer server : serverGroup.getMembers()) {
      ServerNode serverNode = new ServerNode(appContext, clusterModel, server);
      add(serverNode);
    }
    setLabel(appContext.getString("mirror.group") + " (" + serverGroup.getName() + ")");
  }

  protected ServersPanel createServerGroupPanel() {
    return new ServersPanel(appContext, clusterModel, serverGroup.getMembers());
  }

  @Override
  public Component getComponent() {
    if (serverGroupPanel == null) {
      serverGroupPanel = createServerGroupPanel();
    }
    return serverGroupPanel;
  }

  @Override
  public void tearDown() {
    super.tearDown();
    appContext = null;
    serverGroup = null;
    serverGroupPanel = null;
  }
}
