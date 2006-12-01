/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XContainer;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;

public class ClientsPanel extends XContainer {
  private ClientsTable m_table;

  public ClientsPanel(DSOClient[] clients) {
    super(new BorderLayout());
    add(new JScrollPane(m_table = new ClientsTable(clients)));
  }

  public void setClients(DSOClient[] clients) {
    m_table.setClients(clients);
  }
  
  public void add(DSOClient client) {
    m_table.addClient(client);
  }

  public void remove(DSOClient client) {
    m_table.removeClient(client);
  }
}
