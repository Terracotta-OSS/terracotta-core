/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.IClient;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.JScrollPane;
import javax.swing.Timer;

public class ClientsPanel extends XContainer implements ActionListener {
  protected ClientsNode  m_clientsNode;
  protected ClientsTable m_table;
  protected Timer        m_refreshTimer;

  public ClientsPanel(ClientsNode clientsNode, IClient[] clients) {
    super(new BorderLayout());
    setNode(m_clientsNode = clientsNode);
    add(new JScrollPane(m_table = new ClientsTable(clients)));
  }

  public void setClients(IClient[] clients) {
    m_table.setClients(clients);
  }

  public boolean haveAnyClients() {
    return m_table.getModel().getRowCount() > 0;
  }

  public void add(IClient client) {
    m_table.addClient(client);
  }

  public void remove(IClient client) {
    m_table.removeClient(client);
  }

  public void addNotify() {
    super.addNotify();
    m_refreshTimer = new Timer(1000, this);
    m_refreshTimer.setRepeats(false);
    m_refreshTimer.start();
  }
  
  public void removeNotify() {
    super.removeNotify();
    m_refreshTimer.stop();
  }
  
  private class ObjectCountWorker extends BasicWorker<Map<IClient, Integer>> {
    private ObjectCountWorker() {
      super(new Callable<Map<IClient, Integer>>() {
        public Map<IClient, Integer> call() throws Exception {
          return m_clientsNode.getClusterModel().getClientLiveObjectCount();
        }
      });
    }

    public void finished() {
      Exception e = getException();
      if (e == null) {
        m_table.updateObjectCounts(getResult());
        m_refreshTimer.start();
      }
    }
  }
  
  public void actionPerformed(ActionEvent e) {
    if(isShowing()) {
      AdminClient.getContext().executorService.submit(new ObjectCountWorker());
    }
  }

  public void tearDown() {
    if(m_refreshTimer != null) {
      m_refreshTimer.stop();
    }
    
    super.tearDown();

    m_table = null;
    m_clientsNode = null;
    m_refreshTimer = null;
  }
}
