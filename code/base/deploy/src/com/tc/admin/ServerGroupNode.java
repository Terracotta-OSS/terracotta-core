/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;

import java.awt.Component;

public class ServerGroupNode extends ClusterElementNode {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected IServerGroup        serverGroup;
  protected ServersPanel        serverGroupPanel;

  public ServerGroupNode(IAdminClientContext adminClientContext, IClusterModel clusterModel, IServerGroup serverGroup) {
    super(serverGroup);

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    this.serverGroup = serverGroup;

    for (IServer server : serverGroup.getMembers()) {
      ServerNode serverNode = adminClientContext.getNodeFactory().createServerNode(adminClientContext, clusterModel,
                                                                                   server);
      add(serverNode);
    }
    setLabel(serverGroup.getName() + " (" + getChildCount() + ")");

  }

  protected ServersPanel createServerGroupPanel() {
    return new ServersPanel(adminClientContext, clusterModel, serverGroup.getMembers());
  }

  public Component getComponent() {
    if (serverGroupPanel == null) {
      serverGroupPanel = createServerGroupPanel();
    }
    return serverGroupPanel;
  }

  public void tearDown() {
    super.tearDown();
    adminClientContext = null;
    serverGroup = null;
    serverGroupPanel = null;
  }
}
