/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.StatusRenderer;
import com.tc.admin.model.IServer;

import javax.swing.JTable;

public class ClusterMemberStatusRenderer extends StatusRenderer {
  ClusterMemberStatusRenderer() {
    super();
  }

  public void setValue(JTable table, int row, int col) {
    if (!(table instanceof ClusterMemberTable)) { throw new RuntimeException("Not a ClusterMemberTable"); }

    ClusterMemberTableModel clusterMemberTableModel = (ClusterMemberTableModel) table.getModel();
    IServer server = clusterMemberTableModel.getClusterMemberAt(row);

    m_label.setText(server.getName());
    m_indicator.setBackground(ServerNode.getServerStatusColor(server));
  }
}
