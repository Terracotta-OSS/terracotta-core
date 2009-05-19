/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

public class ClientsNode extends ComponentNode implements ClientConnectionListener {
  protected ApplicationContext   appContext;
  protected IClusterModel        clusterModel;
  protected ClusterListener      clusterListener;
  protected ConnectionContext    cc;
  protected IClient[]            clients;
  protected ClientsPanel         clientsPanel;

  private static final IClient[] NULL_CLIENTS = {};

  public ClientsNode(ApplicationContext appContext, IClusterModel clusterModel) {
    super();
    this.appContext = appContext;
    this.clusterModel = clusterModel;
    clients = NULL_CLIENTS;
    setLabel(appContext.getMessage("connected-clients"));
    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      init();
    }
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  private synchronized IServer getActiveCoordinator() {
    IClusterModel theClusterModel = getClusterModel();
    return theClusterModel != null ? theClusterModel.getActiveCoordinator() : null;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      if (clusterModel.isReady()) {
        init();
      }
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(ClientsNode.this);
      }
    }
  }

  private void init() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClientConnectionListener(this);
      setLabel(appContext.getMessage("connected-clients"));
      clients = NULL_CLIENTS;
      for (int i = getChildCount() - 1; i >= 0; i--) {
        removeChild((XTreeNode) getChildAt(i));
      }
      if (clientsPanel != null) {
        clientsPanel.setClients(clients);
      }
      appContext.execute(new InitWorker());
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
          appContext.log(e);
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
    return new ClientNode(appContext, client);
  }

  protected ClientsPanel createClientsPanel(ClientsNode clientsNode, IClient[] theClients) {
    return new ClientsPanel(appContext, getClusterModel(), theClients);
  }

  @Override
  public Component getComponent() {
    if (clientsPanel == null) {
      clientsPanel = createClientsPanel(ClientsNode.this, clients);
    }
    return clientsPanel;
  }

  private void addClientNode(ClientNode clientNode) {
    addChild(clientNode);
    if (clientsPanel != null) {
      clientsPanel.add(clientNode.getClient());
    }
    nodeStructureChanged();
  }

  protected void updateLabel() {
    setLabel(appContext.getMessage("connected-clients") + " (" + getChildCount() + ")");
    nodeChanged();
  }

  @Override
  public void tearDown() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClientConnectionListener(this);
    }
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    if (clientsPanel != null) {
      clientsPanel.tearDown();
    }

    synchronized (this) {
      appContext = null;
      clusterModel = null;
      clusterListener = null;
      cc = null;
      clients = null;
      clientsPanel = null;
    }

    super.tearDown();
  }

  public void clientConnected(IClient client) {
    if (appContext == null) return;
    SwingUtilities.invokeLater(new ClientConnectedRunnable(client));
  }

  private class ClientConnectedRunnable implements Runnable {
    private final IClient client;

    private ClientConnectedRunnable(IClient client) {
      this.client = client;
    }

    public void run() {
      if (appContext == null) return;
      appContext.setStatus(appContext.getMessage("dso.client.retrieving"));
      List<IClient> list = new ArrayList(Arrays.asList(clients));
      list.add(client);
      clients = list.toArray(new IClient[list.size()]);
      addClientNode(createClientNode(client));
      updateLabel();
      appContext.setStatus(appContext.getMessage("dso.client.new") + client);
    }
  }

  public void clientDisconnected(IClient client) {
    if (appContext == null) return;
    SwingUtilities.invokeLater(new ClientDisconnectedRunnable(client));
  }

  private class ClientDisconnectedRunnable implements Runnable {
    private final IClient client;

    private ClientDisconnectedRunnable(IClient client) {
      this.client = client;
    }

    public void run() {
      if (appContext == null) return;
      appContext.setStatus(appContext.getMessage("dso.client.detaching"));
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
      appContext.setStatus(appContext.getMessage("dso.client.detached") + client);
    }
  }

}
