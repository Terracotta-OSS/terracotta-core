/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XContainer;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;

public class ClientsPanel extends XContainer {
  protected ConnectionContext m_cc;
  protected ClientsNode       m_clientsNode;
  protected ClientsTable      m_table;

  public ClientsPanel(ClientsNode clientsNode, DSOClient[] clients) {
    super(new BorderLayout());
    m_cc = clientsNode.getConnectionContext();
    setNode(m_clientsNode = clientsNode);
    add(new JScrollPane(m_table = new ClientsTable(clients)));
  }

  public void setClients(DSOClient[] clients) {
    m_table.setClients(clients);
  }

  public boolean haveAnyClients() {
    return m_table.getModel().getRowCount() > 0;
  }
  
  public void add(DSOClient client) {
    m_table.addClient(client);
  }

  public void remove(DSOClient client) {
    m_table.removeClient(client);
  }

  public void tearDown() {
    super.tearDown();

    m_cc = null;
    m_table = null;
    m_clientsNode = null;
  }
}
