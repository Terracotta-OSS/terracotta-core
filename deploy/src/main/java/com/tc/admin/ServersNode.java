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
  protected final IAdminClientContext adminClientContext;
  protected final IClusterModel       clusterModel;
  protected final IServer[]           servers;

  protected ServersPanel              serversPanel;

  public ServersNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super();

    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    List<IServer> serverList = new ArrayList<IServer>();
    for (IServerGroup serverGroup : clusterModel.getServerGroups()) {
      IServer[] members = serverGroup.getMembers();
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

  @Override
  public Component getComponent() {
    if (serversPanel == null) {
      serversPanel = createServersPanel();
    }
    return serversPanel;
  }

  void selectClientNode(String remoteAddr) {
    // clusterNode.selectClientNode(remoteAddr);
  }
}
