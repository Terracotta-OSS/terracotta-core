/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.IAdminClientContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

public class ClientsNode extends ComponentNode implements ClientConnectionListener, PropertyChangeListener {
  protected IAdminClientContext adminClientContext;
  protected IClusterModel       clusterModel;
  protected ConnectionContext   cc;
  protected IClient[]           clients;
  protected ClientsPanel        clientsPanel;

  public ClientsNode(IAdminClientContext adminClientContext, IClusterModel clusterModel) {
    super();
    this.adminClientContext = adminClientContext;
    this.clusterModel = clusterModel;
    clusterModel.addPropertyChangeListener(this);
    init();
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  private synchronized IServer getActiveCoordinator() {
    IClusterModel theClusterModel = getClusterModel();
    return theClusterModel != null ? theClusterModel.getActiveCoordinator() : null;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    if (IClusterModel.PROP_ACTIVE_COORDINATOR.equals(evt.getPropertyName())) {
      IServer newActive = (IServer) evt.getNewValue();
      if (newActive != null) {
        SwingUtilities.invokeLater(new InitRunnable());
      }
    }
  }

  private class InitRunnable implements Runnable {
    public void run() {
      init();
    }
  }

  private void init() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClientConnectionListener(this);
      setLabel(adminClientContext.getMessage("connected-clients"));
      clients = new IClient[0];
      for (int i = getChildCount() - 1; i >= 0; i--) {
        removeChild((XTreeNode) getChildAt(i));
      }
      if (clientsPanel != null) {
        clientsPanel.setClients(clients);
      }
      adminClientContext.execute(new InitWorker());
    }
  }

  private class InitWorker extends BasicWorker<IClient[]> {
    private InitWorker() {
      super(new Callable<IClient[]>() {
        public IClient[] call() throws Exception {
          IServer activeCoord = getActiveCoordinator();
          if (activeCoord != null) {
            IClient[] result = getClusterModel().getClients();
            activeCoord.addClientConnectionListener(ClientsNode.this);
            return result;
          }
          return IClient.NULL_SET;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          adminClientContext.log(e);
        }
      } else {
        clients = getResult();
        for (int i = 0; i < clients.length; i++) {
          addClientNode(createClientNode(clients[i]));
        }
        updateLabel();
      }
    }
  }

  protected ClientNode createClientNode(IClient client) {
    return new ClientNode(adminClientContext, client);
  }

  protected ClientsPanel createClientsPanel(ClientsNode clientsNode, IClient[] theClients) {
    return new ClientsPanel(adminClientContext, getClusterModel(), theClients);
  }

  @Override
  public Component getComponent() {
    if (clientsPanel == null) {
      clientsPanel = createClientsPanel(ClientsNode.this, clients);
    }
    return clientsPanel;
  }

  public void selectClientNode(String remoteAddr) {
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      ClientNode ctn = (ClientNode) getChildAt(i);
      String ctnRemoteAddr = ctn.getClient().getRemoteAddress();
      if (ctnRemoteAddr.equals(remoteAddr)) {
        adminClientContext.getAdminClientController().select(ctn);
        return;
      }
    }
  }

  private void addClientNode(ClientNode clientNode) {
    addChild(clientNode);
    if (clientsPanel != null) {
      clientsPanel.add(clientNode.getClient());
    }
    nodeStructureChanged();
  }

  protected void updateLabel() {
    setLabel(adminClientContext.getMessage("connected-clients") + " (" + getChildCount() + ")");
    nodeChanged();
  }

  @Override
  public void tearDown() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClientConnectionListener(this);
    }
    clusterModel.removePropertyChangeListener(this);

    if (clientsPanel != null) {
      clientsPanel.tearDown();
      clientsPanel = null;
    }

    adminClientContext = null;
    clusterModel = null;
    cc = null;
    clients = null;

    super.tearDown();
  }

  public void clientConnected(IClient client) {
    if (adminClientContext == null) return;
    SwingUtilities.invokeLater(new ClientConnectedRunnable(client));
  }

  private class ClientConnectedRunnable implements Runnable {
    private final IClient client;

    private ClientConnectedRunnable(IClient client) {
      this.client = client;
    }

    public void run() {
      if (adminClientContext == null) return;
      adminClientContext.setStatus(adminClientContext.getMessage("dso.client.retrieving"));
      List<IClient> list = new ArrayList(Arrays.asList(clients));
      list.add(client);
      clients = list.toArray(new IClient[list.size()]);
      addClientNode(createClientNode(client));
      updateLabel();
      adminClientContext.setStatus(adminClientContext.getMessage("dso.client.new") + client);
    }
  }

  public void clientDisconnected(IClient client) {
    if (adminClientContext == null) return;
    SwingUtilities.invokeLater(new ClientDisconnectedRunnable(client));
  }

  private class ClientDisconnectedRunnable implements Runnable {
    private final IClient client;

    private ClientDisconnectedRunnable(IClient client) {
      this.client = client;
    }

    public void run() {
      if (adminClientContext == null) return;
      adminClientContext.setStatus(adminClientContext.getMessage("dso.client.detaching"));
      ArrayList<IClient> list = new ArrayList<IClient>(Arrays.asList(clients));
      int nodeIndex = list.indexOf(client);
      if (nodeIndex != -1) {
        list.remove(client);
        clients = list.toArray(new IClient[] {});
        removeChild((XTreeNode) getChildAt(nodeIndex));
        if (clientsPanel != null) {
          clientsPanel.remove(client);
        }
      }
      updateLabel();
      adminClientContext.setStatus(adminClientContext.getMessage("dso.client.detached") + client);
    }
  }

}
