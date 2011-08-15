/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XObjectTable;
import com.tc.admin.model.IServer;

import java.awt.Point;
import java.awt.event.MouseEvent;

public class ClusterMemberTable extends XObjectTable {
  public ClusterMemberTable() {
    super();
  }

  public String getToolTipText(MouseEvent event) {
    String tip = null;
    Point p = event.getPoint();
    int row = rowAtPoint(p);

    if (row != -1) {
      ClusterMemberTableModel model = (ClusterMemberTableModel) getModel();
      IServer server = model.getClusterMemberAt(row);
      Exception e = server.getConnectError();

      if (e != null) {
        tip = server.getConnectErrorMessage(e);
      } else {
        tip = server.getConnectionStatusString();
      }
    }

    return tip;
  }
}
