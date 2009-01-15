/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

public class ServersPanel extends XContainer implements PropertyChangeListener {
  protected ApplicationContext      appContext;
  protected IClusterModel           clusterModel;
  protected IServer[]               servers;
  protected XObjectTable            serverTable;
  protected ClusterMemberTableModel serverTableModel;

  public ServersPanel(ApplicationContext appContext, IClusterModel clusterModel, IServer[] servers) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.servers = servers;

    serverTable = new ClusterMemberTable();
    serverTable.setModel(serverTableModel = new ClusterMemberTableModel());
    TableColumnModel colModel = serverTable.getColumnModel();
    colModel.getColumn(0).setCellRenderer(new ClusterMemberStatusRenderer(appContext));
    colModel.getColumn(2).setCellRenderer(new XObjectTable.PortNumberRenderer());

    for (int i = 0; i < servers.length; i++) {
      serverTableModel.addClusterMember(servers[i]);
    }

    add(new XScrollPane(serverTable), BorderLayout.CENTER);

    for(IServer server : servers) {
      server.addPropertyChangeListener(this);
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    final IServer server = (IServer)evt.getSource();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (serverTableModel == null) return;
        int row = serverTableModel.getObjectIndex(server);
        if(row != -1) {
          serverTableModel.fireTableCellUpdated(row, 0);
        } else {
          serverTableModel.fireTableDataChanged();
        }
      }
    });
  }

  public void tearDown() {
    for(IServer server : servers) {
      server.removePropertyChangeListener(this);
    }
    serverTableModel.clear();

    super.tearDown();

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      servers = null;
      serverTable = null;
      serverTableModel = null;
    }
  }
}
