/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.event.EventListenerList;

public class ServerGroup implements IServerGroup {
  private final IClusterModel         clusterModel;
  private final Server[]              members;
  private final boolean               isCoordinator;
  private final String                name;
  private final int                   id;
  private boolean                     connected;
  private boolean                     ready;

  private IServer                     activeServer;
  private final PropertyChangeSupport propertyChangeSupport;
  private ActiveServerListener        activeServerListener;
  private PropertyChangeListener      serverPropertyChangeListener;
  private final EventListenerList     listenerList;

  public ServerGroup(IClusterModel clusterModel, ServerGroupInfo info) {
    this.clusterModel = clusterModel;
    listenerList = new EventListenerList();
    propertyChangeSupport = new PropertyChangeSupport(this);
    serverPropertyChangeListener = new ServerPropertyChangeListener();
    activeServerListener = new ActiveServerListener();

    L2Info[] l2Infos = info.members();
    members = new Server[l2Infos.length];
    for (int i = 0; i < l2Infos.length; i++) {
      members[i] = new Server(getClusterModel(), this, l2Infos[i]);
    }
    name = info.name();
    id = info.id();
    isCoordinator = info.isCoordinator();
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public boolean isCoordinator() {
    return isCoordinator;
  }

  public IServer[] getMembers() {
    return members;
  }

  public String getName() {
    return name;
  }

  public int getId() {
    return id;
  }

  public void setConnectionCredentials(String[] creds) {
    for (IServer server : getMembers()) {
      server.setConnectionCredentials(creds);
    }
  }

  protected void setConnected(boolean connected) {
    boolean oldConnected;
    synchronized (this) {
      oldConnected = isConnected();
      this.connected = connected;
    }
    if (!connected) {
      clearActiveServer();
    }
    firePropertyChange(PROP_CONNECTED, oldConnected, connected);
  }

  public synchronized boolean isConnected() {
    return connected;
  }

  private void setActiveServer(IServer theActiveServer) {
    IServer oldActiveServer = _setActiveServer(theActiveServer);
    if (oldActiveServer != null) {
      oldActiveServer.removePropertyChangeListener(activeServerListener);
    }
    firePropertyChange(PROP_ACTIVE_SERVER, oldActiveServer, theActiveServer);
    if (theActiveServer != null) {
      theActiveServer.addPropertyChangeListener(activeServerListener);
    }
    setReady(determineReady());
  }

  private IServer _setActiveServer(IServer server) {
    IServer oldActiveServer;
    synchronized (this) {
      oldActiveServer = activeServer;
      activeServer = server;
    }
    return oldActiveServer;
  }

  private IServer _clearActiveServer() {
    IServer oldActiveServer;
    synchronized (this) {
      oldActiveServer = activeServer;
      activeServer = null;
    }
    return oldActiveServer;
  }

  private void clearActiveServer() {
    IServer oldActiveServer = _clearActiveServer();
    if (oldActiveServer != null) {
      oldActiveServer.removePropertyChangeListener(activeServerListener);
      firePropertyChange(PROP_ACTIVE_SERVER, oldActiveServer, null);
    }
  }

  public synchronized IServer getActiveServer() {
    return activeServer;
  }

  public void connect() {
    for (IServer member : getMembers()) {
      if (member.isActive()) {
        setActiveServer(member);
      }
      member.addPropertyChangeListener(serverPropertyChangeListener);
    }
  }

  public void disconnect() {
    for (IServer member : getMembers()) {
      if (member.isConnected()) {
        member.disconnect();
      }
    }
    for (IServer member : getMembers()) {
      member.removePropertyChangeListener(serverPropertyChangeListener);
    }
  }

  protected void setReady(boolean ready) {
    boolean oldReady;
    synchronized (this) {
      oldReady = isReady();
      this.ready = ready;
    }
    firePropertyChange(PROP_READY, oldReady, ready);
  }

  public synchronized boolean isReady() {
    return ready;
  }

  private class ServerPropertyChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      String prop = evt.getPropertyName();
      IServer server = (Server) evt.getSource();
      fireServerStateChanged(server, evt);
      if (IServer.PROP_CONNECTED.equals(prop)) {
        if (server.isConnected() && server.isActive()) {
          setActiveServer(server);
        }
        setConnected(determineConnected());
      } else if (IClusterModelElement.PROP_READY.equals(prop)) {
        setReady(determineReady());
      }
    }
  }

  public synchronized void addServerStateListener(ServerStateListener listener) {
    removeServerStateListener(listener);
    listenerList.add(ServerStateListener.class, listener);
  }

  public synchronized void removeServerStateListener(ServerStateListener listener) {
    listenerList.remove(ServerStateListener.class, listener);
  }

  protected void fireServerStateChanged(IServer server, PropertyChangeEvent pce) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ServerStateListener.class) {
        ((ServerStateListener) listeners[i + 1]).serverStateChanged(server, pce);
      }
    }
  }

  private class ActiveServerListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      String prop = evt.getPropertyName();
      if (IClusterModelElement.PROP_READY.equals(prop)) {
        IServer server = (IServer) evt.getSource();
        if (!server.isReady()) {
          clearActiveServer();
        }
      }
    }
  }

  public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null && propertyChangeSupport != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
      propertyChangeSupport.addPropertyChangeListener(listener);
    }
  }

  public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null && propertyChangeSupport != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
    }
  }

  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    PropertyChangeSupport pcs;
    synchronized (this) {
      pcs = propertyChangeSupport;
    }
    if (pcs != null && (oldValue != null || newValue != null)) {
      pcs.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  public synchronized void tearDown() {
    if (members != null) {
      for (IServer server : members) {
        server.removePropertyChangeListener(serverPropertyChangeListener);
        server.tearDown();
      }
    }
    serverPropertyChangeListener = null;
    activeServerListener = null;
    activeServer = null;
  }

  @Override
  public String toString() {
    return dump();
  }

  public String dump() {
    StringBuilder sb = new StringBuilder();
    sb.append("name=");
    sb.append(name);
    sb.append(", id=");
    sb.append(id);
    sb.append(", connected=");
    sb.append(connected);
    sb.append(", ready=");
    sb.append(ready);
    sb.append(", isCoordinator=");
    sb.append(isCoordinator);
    sb.append(", members=");
    for (int i = 0; i < members.length; i++) {
      sb.append("member");
      sb.append(i);
      sb.append("[");
      sb.append(members[i].dump());
      sb.append("], ");
    }
    sb.append("activeServer=");
    sb.append(getActiveServer());
    return sb.toString();
  }

  private boolean determineReady() {
    IServer theActiveServer = getActiveServer();
    return theActiveServer != null && theActiveServer.isReady();
  }

  private boolean determineConnected() {
    for (IServer server : getMembers()) {
      if (server.isConnected()) { return true; }
    }
    return false;
  }
}
