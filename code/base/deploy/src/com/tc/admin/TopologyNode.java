/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextPane;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.dso.DSOHelper;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;

import javax.swing.Icon;

public class TopologyNode extends ClusterElementNode {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected XScrollPane         topologyPanel;
  protected ClientsNode         clientsNode;

  public TopologyNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super(clusterModel);

    setLabel(adminClientContext.getString("cluster.topology"));
    
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;

    add(clientsNode = createClientsNode());
    add(createServerGroupsNode());
  }

  protected ServerGroupsNode createServerGroupsNode() {
    return new ServerGroupsNode(adminClientContext, getClusterModel());
  }

  protected ClientsNode createClientsNode() {
    return new ClientsNode(adminClientContext, getClusterModel());
  }

  synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  public void selectClientNode(String remoteAddr) {
    clientsNode.selectClientNode(remoteAddr);
  }

  public Component getComponent() {
    if (topologyPanel == null) {
      XTextPane textPane = new XTextPane();
      topologyPanel = new XScrollPane(textPane);
      try {
        textPane.setPage(getClass().getResource("TopologyIntro.html"));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return topologyPanel;
  }

  public Icon getIcon() {
    return DSOHelper.getHelper().getTopologyIcon();
  }

  public void tearDown() {
    super.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterModel = null;
      topologyPanel = null;
      clientsNode = null;
    }
  }
}
