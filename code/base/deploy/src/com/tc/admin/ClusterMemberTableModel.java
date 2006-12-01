/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XObjectTableModel;
  
public class ClusterMemberTableModel extends XObjectTableModel {
  private static final String[] CLUSTER_MEMBERS_FIELDS  = {"Name", "Hostname", "JMXPortNumber"};
  private static final String[] CLUSTER_MEMBERS_HEADERS = {"Name", "Host",     "Admin Port"};

  public ClusterMemberTableModel() {
    super(ServerConnectionManager.class,
          CLUSTER_MEMBERS_FIELDS,
          CLUSTER_MEMBERS_HEADERS);
  }
  
  public void addClusterMember(ServerConnectionManager scm) {
    add(scm);
  }
  
  public ServerConnectionManager getClusterMemberAt(int row) {
    return (ServerConnectionManager)getObjectAt(row);
  }
  
  public void tearDown() {
    for(int i = 0; i < getRowCount(); i++) {
      getClusterMemberAt(i).tearDown();
    }
    clear();
  }
}

