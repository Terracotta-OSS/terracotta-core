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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.event.EventListenerList;

public class ServerGroup implements IServerGroup {
  private final IClusterModel            clusterModel;
  protected final Server[]               members;
  private final boolean                  isCoordinator;
  private final String                   name;
  private final int                      id;
  private final AtomicBoolean            connected    = new AtomicBoolean();
  private final AtomicBoolean            ready        = new AtomicBoolean();
  private final AtomicReference<IServer> activeServer = new AtomicReference<IServer>();
  private final PropertyChangeSupport    propertyChangeSupport;
  private final ActiveServerListener     activeServerListener;
  private final PropertyChangeListener   serverPropertyChangeListener;
  private final EventListenerList        listenerList;

  public ServerGroup(IClusterModel clusterModel, ServerGroupInfo info) {
    this.clusterModel = clusterModel;
    listenerList = new EventListenerList();
    propertyChangeSupport = new PropertyChangeSupport(this);
    serverPropertyChangeListener = new ServerPropertyChangeListener();
    activeServerListener = new ActiveServerListener();

    L2Info[] l2Infos = info.members();
    members = new Server[l2Infos.length];
    initMembers(l2Infos);
    name = info.name();
    id = info.id();
    isCoordinator = info.isCoordinator();
  }

  protected void initMembers(L2Info[] l2Infos) {
    for (int i = 0; i < l2Infos.length; i++) {
      members[i] = new Server(getClusterModel(), this, l2Infos[i]);
    }
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

  public void clearConnectionCredentials() {
    for (IServer server : getMembers()) {
      server.clearConnectionCredentials();
    }
  }

  protected void setConnected(boolean newConnected) {
    if (connected.compareAndSet(!newConnected, newConnected)) {
      if (!newConnected) {
        clearActiveServer();
      }
      firePropertyChange(PROP_CONNECTED, !newConnected, newConnected);
    }
  }

  public boolean isConnected() {
    return connected.get();
  }

  private void setActiveServer(IServer newActiveServer) {
    IServer oldActiveServer = getActiveServer();
    if (oldActiveServer != null) {
      if (oldActiveServer == newActiveServer) { return; }
      if (oldActiveServer.isActive()) {
        oldActiveServer.splitbrain();
        newActiveServer.splitbrain();
        return;
      }
      oldActiveServer.removePropertyChangeListener(activeServerListener);
    }
    if (activeServer.compareAndSet(oldActiveServer, newActiveServer)) {
      firePropertyChange(PROP_ACTIVE_SERVER, oldActiveServer, newActiveServer);
      if (newActiveServer != null) {
        newActiveServer.addPropertyChangeListener(activeServerListener);
      }
      setReady(determineReady());
    }
  }

  private void clearActiveServer() {
    IServer oldActiveServer;
    if (activeServer.compareAndSet(oldActiveServer = activeServer.get(), null)) {
      if (oldActiveServer != null) {
        oldActiveServer.removePropertyChangeListener(activeServerListener);
        firePropertyChange(PROP_ACTIVE_SERVER, oldActiveServer, null);
      }
    }
  }

  public IServer getActiveServer() {
    return activeServer.get();
  }

  public void connect() {
    for (IServer member : getMembers()) {
      member.addPropertyChangeListener(serverPropertyChangeListener);
    }
    for (IServer member : getMembers()) {
      if (member.isActive()) {
        setActiveServer(member);
      }
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

  protected void setReady(boolean newReady) {
    if (ready.compareAndSet(!newReady, newReady)) {
      firePropertyChange(PROP_READY, !newReady, newReady);
    }
  }

  public boolean isReady() {
    return ready.get();
  }

  private class ServerPropertyChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      String prop = evt.getPropertyName();
      IServer server = (Server) evt.getSource();
      fireServerStateChanged(server, evt);
      if (IServer.PROP_CONNECTED.equals(prop)) {
        setConnected(determineConnected());
        if (server.isConnected() && server.isActive()) {
          setActiveServer(server);
        }
      } else if (IClusterModelElement.PROP_READY.equals(prop)) {
        if (server.isConnected() && server.isActive()) {
          setActiveServer(server);
          setReady(determineReady());
        }
      }
    }
  }

  public void addServerStateListener(ServerStateListener listener) {
    removeServerStateListener(listener);
    listenerList.add(ServerStateListener.class, listener);
  }

  public void removeServerStateListener(ServerStateListener listener) {
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
        IServer theActiveServer = getActiveServer();
        if ((server == theActiveServer) && !server.isReady()) {
          clearActiveServer();
        }
      }
    }
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
      propertyChangeSupport.addPropertyChangeListener(listener);
    }
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
    }
  }

  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    if (oldValue != null || newValue != null) {
      propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  public void tearDown() {
    if (members != null) {
      for (IServer server : members) {
        server.removePropertyChangeListener(serverPropertyChangeListener);
        server.tearDown();
      }
    }
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
    boolean isActiveReady = false;
    int activeCount = 0;
    for (IServer server : getMembers()) {
      if (server.isActive()) {
        activeCount++;
        isActiveReady = server.isReady();
      }
    }
    return activeCount == 1 && isActiveReady;
  }

  private boolean determineConnected() {
    for (IServer server : getMembers()) {
      if (server.isConnected()) { return true; }
    }
    return false;
  }
}
