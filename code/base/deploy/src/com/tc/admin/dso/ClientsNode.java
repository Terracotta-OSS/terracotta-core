/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ClusterNode;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

public class ClientsNode extends ComponentNode implements ClientConnectionListener, PropertyChangeListener {
  protected AdminClientContext m_acc;
  protected ClusterNode        m_clusterNode;
  protected ConnectionContext  m_cc;
  protected IClient[]          m_clients;
  protected ClientsPanel       m_clientsPanel;

  public ClientsNode(ClusterNode clusterNode) {
    super();
    m_acc = AdminClient.getContext();
    m_clusterNode = clusterNode;
    clusterNode.getClusterModel().addPropertyChangeListener(this);
    init();
  }

  IClusterModel getClusterModel() {
    return getClusterNode().getClusterModel();
  }
  
  ClusterNode getClusterNode() {
    return m_clusterNode;
  }
  
  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModel.PROP_ACTIVE_SERVER.equals(prop)) {
      SwingUtilities.invokeLater(new InitRunnable());
    }
  }

  private class InitRunnable implements Runnable {
    public void run() {
      init();
    }
  }

  private void init() {
    m_clusterNode.getClusterModel().removeClientConnectionListener(this);
    setLabel(m_acc.getMessage("clients"));
    m_clients = new IClient[0];
    for (int i = getChildCount() - 1; i >= 0; i--) {
      m_acc.controller.remove((XTreeNode) getChildAt(i));
    }
    if (m_clientsPanel != null) {
      m_clientsPanel.setClients(m_clients);
    }
    m_acc.executorService.execute(new InitWorker());
  }

  private class InitWorker extends BasicWorker<IClient[]> {
    private InitWorker() {
      super(new Callable<IClient[]>() {
        public IClient[] call() throws Exception {
          IClient[] result = getClusterModel().getClients();
          getClusterModel().addClientConnectionListener(ClientsNode.this);
          return result;
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        m_acc.log(e);
      } else {
        m_clients = getResult();
        for (int i = 0; i < m_clients.length; i++) {
          addClientNode(createClientNode(m_clients[i]));
        }
        updateLabel();
      }
    }
  }

  protected ClientNode createClientNode(IClient client) {
    return new ClientNode(this, client);
  }

  protected ClientsPanel createClientsPanel(ClientsNode clientsNode, IClient[] clients) {
    return new ClientsPanel(this, clients);
  }

  public Component getComponent() {
    if (m_clientsPanel == null) {
      m_clientsPanel = createClientsPanel(ClientsNode.this, m_clients);
    }
    return m_clientsPanel;
  }

  public void selectClientNode(String remoteAddr) {
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      ClientNode ctn = (ClientNode) getChildAt(i);
      String ctnRemoteAddr = ctn.getClient().getRemoteAddress();
      if (ctnRemoteAddr.equals(remoteAddr)) {
        m_acc.controller.select(ctn);
        return;
      }
    }
  }

  private void addClientNode(ClientNode clientNode) {
    XTreeModel model = getModel();
    if (model != null) {
      model.insertNodeInto(clientNode, this, getChildCount());
    } else {
      add(clientNode);
    }
    if (m_clientsPanel != null) {
      m_clientsPanel.add(clientNode.getClient());
    }
  }

  private void updateLabel() {
    setLabel(m_acc.getMessage("clients") + " (" + getChildCount() + ")");
    nodeChanged();
  }

  public void tearDown() {
    getClusterModel().removeClientConnectionListener(this);
    getClusterModel().removePropertyChangeListener(this);

    if (m_clientsPanel != null) {
      m_clientsPanel.tearDown();
      m_clientsPanel = null;
    }

    m_acc = null;
    m_clusterNode = null;
    m_cc = null;
    m_clients = null;

    super.tearDown();
  }

  public void clientConnected(IClient client) {
    if (m_acc == null) return;
    SwingUtilities.invokeLater(new ClientConnectedRunnable(client));
  }

  private class ClientConnectedRunnable implements Runnable {
    private IClient m_client;

    private ClientConnectedRunnable(IClient client) {
      m_client = client;
    }

    public void run() {
      if (m_acc == null) return;
      m_acc.setStatus(m_acc.getMessage("dso.client.retrieving"));
      List<IClient> list = new ArrayList(Arrays.asList(m_clients));
      list.add(m_client);
      m_clients = list.toArray(new IClient[list.size()]);
      addClientNode(createClientNode(m_client));
      updateLabel();
      m_acc.setStatus(m_acc.getMessage("dso.client.new") + m_client);
    }
  }

  public void clientDisconnected(IClient client) {
    if (m_acc == null) return;
    SwingUtilities.invokeLater(new ClientDisconnectedRunnable(client));
  }

  private class ClientDisconnectedRunnable implements Runnable {
    private IClient m_client;

    private ClientDisconnectedRunnable(IClient client) {
      m_client = client;
    }

    public void run() {
      if (m_acc == null) return;
      m_acc.setStatus(m_acc.getMessage("dso.client.detaching"));
      ArrayList<IClient> list = new ArrayList<IClient>(Arrays.asList(m_clients));
      int nodeIndex = list.indexOf(m_client);
      if (nodeIndex != -1) {
        list.remove(m_client);
        m_clients = list.toArray(new IClient[] {});
        m_acc.controller.remove((XTreeNode) getChildAt(nodeIndex));
        if (m_clientsPanel != null) {
          m_clientsPanel.remove(m_client);
        }
      }
      updateLabel();
      m_acc.setStatus(m_acc.getMessage("dso.client.detached") + m_client);
    }
  }

}
