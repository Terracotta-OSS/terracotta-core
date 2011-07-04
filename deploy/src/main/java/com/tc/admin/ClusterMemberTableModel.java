/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.model.IServer;

public class ClusterMemberTableModel extends XObjectTableModel {
  private static final String[] CLUSTER_MEMBERS_FIELDS  = { "Name", "Host", "Port" };
  private static final String[] CLUSTER_MEMBERS_HEADERS = { "Name", "Host", "Admin Port" };

  public ClusterMemberTableModel() {
    super(IServer.class, CLUSTER_MEMBERS_FIELDS, CLUSTER_MEMBERS_HEADERS);
  }

  public void addClusterMember(IServer server) {
    add(server);
  }

  public IServer getClusterMemberAt(int row) {
    return (IServer) getObjectAt(row);
  }

  public void tearDown() {
    clear();
  }
}
