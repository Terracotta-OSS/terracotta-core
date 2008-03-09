/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.ContainerResource;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;

import javax.swing.table.TableColumnModel;

public class ServersPanel extends XContainer {
  private ServersNode             m_serversNode;
  private XObjectTable            m_clusterMemberTable;
  private ClusterMemberTableModel m_clusterMemberTableModel;
 
  public ServersPanel(ServersNode serversNode) {
    super();

    m_serversNode = serversNode;

    load((ContainerResource) AdminClient.getContext().topRes.getComponent("ServersPanel"));

    m_clusterMemberTable = (XObjectTable) findComponent("ClusterMembersTable");
    m_clusterMemberTableModel = new ClusterMemberTableModel();
    m_clusterMemberTable.setModel(m_clusterMemberTableModel);
    TableColumnModel colModel = m_clusterMemberTable.getColumnModel();
    colModel.getColumn(0).setCellRenderer(new ClusterMemberStatusRenderer());
    colModel.getColumn(2).setCellRenderer(new XObjectTable.PortNumberRenderer());
    
    for(int i = 0; i < m_serversNode.getChildCount(); i++) {
      ServerNode serverNode = (ServerNode)m_serversNode.getChildAt(i);
      m_clusterMemberTableModel.addClusterMember(serverNode.getServerConnectionManager());
    }
  }
}
