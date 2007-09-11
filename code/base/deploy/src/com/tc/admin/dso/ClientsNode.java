/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XTreeNode;
import com.tc.stats.DSOMBean;

import java.util.ArrayList;
import java.util.Arrays;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.SwingUtilities;

public class ClientsNode extends ComponentNode
  implements NotificationListener
{
  private ConnectionContext m_cc;
  private DSOClient[]       m_clients;

  public ClientsNode(ConnectionContext cc) throws Exception {
    super();

    m_cc = cc;
    
    ObjectName dso = DSOHelper.getHelper().getDSOMBean(m_cc);
    m_clients = ClientsHelper.getHelper().getClients(m_cc);

    for(int i = 0; i < m_clients.length; i++) {
      add(new ClientTreeNode(m_cc, m_clients[i]));
    }

    ClientsPanel panel = new ClientsPanel(m_clients);

    panel.setNode(this);
    setLabel(AdminClient.getContext().getMessage("clients"));
    setComponent(panel);

    m_cc.addNotificationListener(dso, this);
  }

  public DSOClient[] getClients() {
    return m_clients;
  }

  public void handleNotification(final Notification notice, final Object handback) {
    String type = notice.getType();

    if (DSOMBean.CLIENT_ATTACHED.equals(type) || DSOMBean.CLIENT_DETACHED.equals(type)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          internalHandleNotification(notice, handback);
        }
      });
    }
  }
  
  private void internalHandleNotification(Notification notice, Object handback) {
    String type = notice.getType();

    if(DSOMBean.CLIENT_ATTACHED.equals(type)) {
      AdminClientContext acc = AdminClient.getContext();

      acc.setStatus(acc.getMessage("dso.client.retrieving"));

      ObjectName clientObjectName = (ObjectName)notice.getSource();
      DSOClient  client           = new DSOClient(m_cc, clientObjectName);
      ArrayList  list             = new ArrayList(Arrays.asList(m_clients));

      list.add(client);
      m_clients = (DSOClient[])list.toArray(new DSOClient[]{});

      ClientTreeNode ctn = new ClientTreeNode(m_cc, client);
      getModel().insertNodeInto(ctn, this, getChildCount());

      ((ClientsPanel)getComponent()).add(client);

      acc.setStatus(acc.getMessage("dso.client.new") + client);
    } else if(DSOMBean.CLIENT_DETACHED.equals(type)) {
      AdminClientContext acc = AdminClient.getContext();

      acc.setStatus(acc.getMessage("dso.client.detaching"));

      int        nodeIndex        = -1;
      ObjectName clientObjectName = (ObjectName)notice.getSource();
      ArrayList  list             = new ArrayList(Arrays.asList(m_clients));
      DSOClient  client           = null;

      for(int i = 0; i < list.size(); i++) {
        client = (DSOClient)list.get(i);

        if(client.getObjectName().equals(clientObjectName)) {
          list.remove(client);
          m_clients = (DSOClient[])list.toArray(new DSOClient[]{});
          nodeIndex = i;
          break;
        }
      }

      if(nodeIndex != -1) {
        acc.controller.remove((XTreeNode)getChildAt(nodeIndex));
        ((ClientsPanel)getComponent()).remove(client);
      }

      acc.setStatus(acc.getMessage("dso.client.detached") + client);
    }
  }
  
  public void tearDown() {
    try {
      ObjectName dso = DSOHelper.getHelper().getDSOMBean(m_cc);
      if(dso != null) {
        m_cc.removeNotificationListener(dso, this);
      }
    } catch(Exception e) {/**/}
    m_cc = null;
    m_clients = null;
    
    super.tearDown();
  }
}
