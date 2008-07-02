/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XObjectTable;
import com.tc.admin.model.IClient;

import java.util.Map;

public class ClientsTable extends XObjectTable {
  private ClientTableModel m_model;

  public ClientsTable() {
    super();
    setModel(m_model = new ClientTableModel());
    getColumnModel().getColumn(1).setCellRenderer(new XObjectTable.PortNumberRenderer());
  }

  public ClientsTable(IClient[] clients) {
    this();
    setClients(clients);
  }

  public void setClients(IClient[] clients) {
    m_model.setClients(clients);
  }

  public void addClient(IClient client) {
    m_model.addClient(client);
  }

  public void removeClient(IClient client) {
    m_model.removeClient(client);
  }
  
  void updateObjectCounts(Map<IClient, Integer> map) {
    m_model.updateObjectCounts(map);
  }
}
