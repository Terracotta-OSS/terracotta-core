/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.Component;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ClusterNode;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.stats.DSOMBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.SwingUtilities;

public class ClientsNode extends ComponentNode implements NotificationListener {
  protected AdminClientContext m_acc;
  protected ClusterNode        m_clusterNode;
  protected ConnectionContext  m_cc;
  protected DSOClient[]        m_clients;
  protected ClientsPanel       m_clientsPanel;

  public ClientsNode(ClusterNode clusterNode) {
    super();
    m_acc = AdminClient.getContext();
    m_clusterNode = clusterNode;
    init();
  }

  private void init() {
    setLabel(m_acc.getMessage("clients"));
    m_clients = new DSOClient[0];
    for (int i = getChildCount() - 1; i >= 0; i--) {
      m_acc.controller.remove((XTreeNode) getChildAt(i));
    }
    if (m_clientsPanel != null) {
      m_clientsPanel.setClients(m_clients);
    }
    m_acc.executorService.execute(new InitWorker());
  }

  private class InitWorker extends BasicWorker<DSOClient[]> {
    private InitWorker() {
      super(new Callable<DSOClient[]>() {
        public DSOClient[] call() throws Exception {
          m_cc = m_clusterNode.getConnectionContext();
          ObjectName dso = DSOHelper.getHelper().getDSOMBean(m_cc);
          m_cc.addNotificationListener(dso, ClientsNode.this);
          return ClientsHelper.getHelper().getClients(m_cc);
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
          addClientNode(createClientNode(m_cc, m_clients[i]));
        }
        updateLabel();
      }
    }
  }

  protected ClientNode createClientNode(ConnectionContext cc, DSOClient client) {
    return new ClientNode(cc, client);
  }

  protected ClientsPanel createClientsPanel(ConnectionContext cc, ClientsNode clientsNode, DSOClient[] clients) {
    return new ClientsPanel(cc, this, clients);
  }

  public Component getComponent() {
    if (m_clientsPanel == null) {
      m_clientsPanel = createClientsPanel(m_cc, ClientsNode.this, m_clients);
    }
    return m_clientsPanel;
  }

  public void newConnectionContext() {
    init();
  }

  void selectClientNode(String remoteAddr) {
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

  public DSOClient[] getClients() {
    return m_clients;
  }

  private boolean haveClient(ObjectName objectName) {
    if (m_clients == null) return false;
    for (DSOClient client : m_clients) {
      if (client.getObjectName().equals(objectName)) { return true; }
    }
    return false;
  }

  public void handleNotification(final Notification notice, final Object handback) {
    String type = notice.getType();

    if (DSOMBean.CLIENT_ATTACHED.equals(type) || DSOMBean.CLIENT_DETACHED.equals(type)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          internalHandleNotification(notice, handback);
          updateLabel();
        }
      });
    }
  }

  private void updateLabel() {
    setLabel(m_acc.getMessage("clients") + " (" + getChildCount() + ")");
    nodeChanged();
  }

  private void internalHandleNotification(Notification notice, Object handback) {
    String type = notice.getType();

    if (DSOMBean.CLIENT_ATTACHED.equals(type)) {
      m_acc.setStatus(m_acc.getMessage("dso.client.retrieving"));

      ObjectName clientObjectName = (ObjectName) notice.getSource();
      if (haveClient(clientObjectName)) return;

      DSOClient client = new DSOClient(m_cc, clientObjectName);
      ArrayList<DSOClient> list = new ArrayList<DSOClient>(Arrays.asList(m_clients));

      list.add(client);
      m_clients = list.toArray(new DSOClient[0]);
      addClientNode(createClientNode(m_cc, client));

      m_acc.setStatus(m_acc.getMessage("dso.client.new") + client);
    } else if (DSOMBean.CLIENT_DETACHED.equals(type)) {
      m_acc.setStatus(m_acc.getMessage("dso.client.detaching"));

      int nodeIndex = -1;
      ObjectName clientObjectName = (ObjectName) notice.getSource();
      ArrayList<DSOClient> list = new ArrayList<DSOClient>(Arrays.asList(m_clients));
      DSOClient client = null;

      for (int i = 0; i < list.size(); i++) {
        client = list.get(i);

        if (client.getObjectName().equals(clientObjectName)) {
          list.remove(client);
          m_clients = list.toArray(new DSOClient[] {});
          nodeIndex = i;
          break;
        }
      }

      if (nodeIndex != -1) {
        m_acc.controller.remove((XTreeNode) getChildAt(nodeIndex));
        if (m_clientsPanel != null) {
          m_clientsPanel.remove(client);
        }
      }

      m_acc.setStatus(m_acc.getMessage("dso.client.detached") + client);
    }
  }

  public void tearDown() {
    try {
      ObjectName dso = DSOHelper.getHelper().getDSOMBean(m_cc);
      if (dso != null) {
        m_cc.removeNotificationListener(dso, this);
      }
    } catch (Exception e) {/**/
    }

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
}
