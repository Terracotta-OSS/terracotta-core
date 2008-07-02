/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.ContainerResource;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.ServerStateListener;

import java.beans.PropertyChangeEvent;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

public class ServersPanel extends XContainer implements ServerStateListener {
  protected AdminClientContext      m_acc;
  protected ServersNode             m_serversNode;
  protected ConnectionContext       m_connectionContext;
  protected XObjectTable            m_clusterMemberTable;
  protected ClusterMemberTableModel m_clusterMemberTableModel;

  public ServersPanel(ServersNode serversNode) {
    super();

    m_acc = AdminClient.getContext();
    m_serversNode = serversNode;
    m_connectionContext = serversNode.getConnectionContext();

    load((ContainerResource) m_acc.topRes.getComponent("ServersPanel"));

    m_clusterMemberTable = (XObjectTable) findComponent("ClusterMembersTable");
    m_clusterMemberTableModel = new ClusterMemberTableModel();
    m_clusterMemberTable.setModel(m_clusterMemberTableModel);
    TableColumnModel colModel = m_clusterMemberTable.getColumnModel();
    colModel.getColumn(0).setCellRenderer(new ClusterMemberStatusRenderer());
    colModel.getColumn(2).setCellRenderer(new XObjectTable.PortNumberRenderer());

    for (int i = 0; i < m_serversNode.getChildCount(); i++) {
      ServerNode serverNode = (ServerNode) m_serversNode.getChildAt(i);
      m_clusterMemberTableModel.addClusterMember(serverNode.getServer());
    }
    
    serversNode.getClusterModel().addServerStateListener(this);
  }

  IClusterModel getClusterModel() {
    return m_serversNode.getClusterModel();
  }
  
  public void serverStateChanged(final IServer server, PropertyChangeEvent e) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        int row = m_clusterMemberTableModel.getObjectIndex(server);
        m_clusterMemberTableModel.fireTableCellUpdated(row, 0);
      }
    });
  }

  public void tearDown() {
    super.tearDown();

    m_acc = null;
    m_serversNode = null;
    m_connectionContext = null;
    m_clusterMemberTable = null;
    m_clusterMemberTableModel = null;
  }
}
