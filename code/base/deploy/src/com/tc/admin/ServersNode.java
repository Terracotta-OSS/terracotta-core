/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class ServersNode extends ComponentNode {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected IServer[]           servers;
  protected ServersPanel        serversPanel;

  public ServersNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super();
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    IServerGroup[] serverGroups = clusterModel.getServerGroups();
    List<IServer> serverList = new ArrayList<IServer>();
    for (IServerGroup group : serverGroups) {
      IServer[] members = group.getMembers();
      for (IServer server : members) {
        ServerNode serverNode = new ServerNode(adminClientContext, clusterModel, server);
        add(serverNode);
        serverList.add(server);
      }
    }
    servers = serverList.toArray(IServer.NULL_SET);
    setLabel(adminClientContext.getMessage("servers") + " (" + getChildCount() + ")");
  }

  protected ServersPanel createServersPanel() {
    return new ServersPanel(adminClientContext, clusterModel, servers);
  }

  public Component getComponent() {
    if (serversPanel == null) {
      serversPanel = createServersPanel();
    }
    return serversPanel;
  }

  void selectClientNode(String remoteAddr) {
    // clusterNode.selectClientNode(remoteAddr);
  }

  public void tearDown() {
    super.tearDown();
    adminClientContext = null;
    clusterModel = null;
    servers = null;
  }
}
