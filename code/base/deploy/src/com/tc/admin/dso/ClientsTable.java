/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XObjectTable;

public class ClientsTable extends XObjectTable {
  private ClientTableModel m_model;

  public ClientsTable() {
    super();
    setModel(m_model = new ClientTableModel());
  }

  public ClientsTable(DSOClient[] clients) {
    this();
    setClients(clients);
  }

  public void setClients(DSOClient[] clients) {
    m_model.set(clients);
  }

  public void addClient(DSOClient client) {
    m_model.add(client);
    
    int row = m_model.getRowCount()-1;
    m_model.fireTableRowsInserted(row, row);
  }

  public void removeClient(DSOClient client) {
    int row = m_model.getObjectIndex(client);

    if(row != -1) {
      m_model.remove(client);
      m_model.fireTableRowsDeleted(row, row);
    }
  }
}
