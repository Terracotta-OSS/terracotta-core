/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServerGroup;

import java.awt.BorderLayout;

public class ServerGroupsPanel extends XContainer {
  protected ApplicationContext    appContext;
  protected IClusterModel         clusterModel;
  protected IServerGroup[]        serverGroups;
  protected XObjectTable          serverGroupTable;
  protected ServerGroupTableModel serverGroupTableModel;

  public ServerGroupsPanel(ApplicationContext appContext, IClusterModel clusterModel, IServerGroup[] serverGroups) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.serverGroups = serverGroups;

    serverGroupTable = new XObjectTable();
    serverGroupTableModel = new ServerGroupTableModel();
    serverGroupTable.setModel(serverGroupTableModel);

    for (int i = 0; i < serverGroups.length; i++) {
      serverGroupTableModel.addGroup(serverGroups[i]);
    }

    add(new XScrollPane(serverGroupTable), BorderLayout.CENTER);
  }

  private static final String[] FIELDS  = { "Name", "Id" };
  private static final String[] HEADERS = { "Group Name", "Group Id" };

  private class ServerGroupTableModel extends XObjectTableModel {
    ServerGroupTableModel() {
      super(IServerGroup.class, FIELDS, HEADERS);
    }

    void addGroup(IServerGroup group) {
      add(group);
    }
  }

  public void tearDown() {
    serverGroupTableModel.clear();

    super.tearDown();

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      serverGroups = null;
      serverGroupTable = null;
      serverGroupTableModel = null;
    }
  }
}
