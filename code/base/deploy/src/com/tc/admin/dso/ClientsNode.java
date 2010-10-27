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
import java.util.concurrent.atomic.AtomicBoolean;

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
      if (tornDown.get()) { return; }
      if (clusterModel.isReady()) {
        init();
      } else {
        suspend();
      }
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (tornDown.get()) { return; }
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(ClientsNode.this);
      }
      if (newActive != null) {
        newActive.addClientConnectionListener(ClientsNode.this);
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      if (appContext != null) {
        appContext.log(e);
      } else {
        super.handleUncaughtError(e);
      }
    }
  }

  private void init() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClientConnectionListener(this);
      removeAllClients();
      appContext.execute(new InitWorker());
    }
  }

  private void suspend() {
    removeAllClients();
  }

  private void removeAllClients() {
    setLabel(appContext.getMessage("connected-clients"));
    clients = NULL_CLIENTS;
    tearDownChildren();
    if (clientsPanel != null) {
      clientsPanel.setClients(clients);
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
      if (tornDown.get()) { return; }
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
      } else {
        tearDownChildren();
        clients = getResult();
        for (IClient client : clients) {
          addClientNode(createClientNode(client));
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

  public void clientConnected(IClient client) {
    if (!tornDown.get()) {
      SwingUtilities.invokeLater(new ClientConnectedRunnable(client));
    }
  }

  private class ClientConnectedRunnable implements Runnable {
    private final IClient client;

    private ClientConnectedRunnable(IClient client) {
      this.client = client;
    }

    private boolean haveClient(IClient newClient) {
      for (IClient c : clients) {
        if (c.getRemoteAddress().equals(newClient.getRemoteAddress())) { return true; }
      }
      return false;
    }

    public void run() {
      if (tornDown.get()) { return; }
      if (!haveClient(client)) {
        appContext.setStatus(appContext.getMessage("dso.client.retrieving"));
        List<IClient> list = new ArrayList(Arrays.asList(clients));
        list.add(client);
        clients = list.toArray(new IClient[list.size()]);
        addClientNode(createClientNode(client));
        updateLabel();
        appContext.setStatus(appContext.getMessage("dso.client.new") + client);
      }
    }
  }

  public void clientDisconnected(IClient client) {
    if (!tornDown.get()) {
      SwingUtilities.invokeLater(new ClientDisconnectedRunnable(client));
    }
  }

  private class ClientDisconnectedRunnable implements Runnable {
    private final IClient client;

    private ClientDisconnectedRunnable(IClient client) {
      this.client = client;
    }

    public void run() {
      if (tornDown.get()) { return; }
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

  private final AtomicBoolean tornDown = new AtomicBoolean(false);

  @Override
  public void tearDown() {
    if (!tornDown.compareAndSet(false, true)) { return; }

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

}
