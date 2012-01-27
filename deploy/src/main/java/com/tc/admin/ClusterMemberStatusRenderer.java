/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.AbstractTableCellRenderer;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.StatusView;
import com.tc.admin.model.IServer;

import javax.swing.JComponent;
import javax.swing.JTable;

public class ClusterMemberStatusRenderer extends AbstractTableCellRenderer {
  private final StatusView statusView;

  ClusterMemberStatusRenderer(ApplicationContext appContext) {
    super();
    statusView = new StatusView();
  }

  @Override
  public JComponent getComponent() {
    return statusView;
  }

  @Override
  public void setValue(JTable table, int row, int col) {
    if (!(table instanceof ClusterMemberTable)) { throw new RuntimeException("Not a ClusterMemberTable"); }

    ClusterMemberTableModel clusterMemberTableModel = (ClusterMemberTableModel) table.getModel();
    IServer server = clusterMemberTableModel.getClusterMemberAt(row);

    ServerHelper.getHelper().setStatusView(server, statusView);
    statusView.getLabel().setText(server.getName());
  }
}
