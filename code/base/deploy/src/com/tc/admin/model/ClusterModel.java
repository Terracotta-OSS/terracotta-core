/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.ConnectionContext;
import com.tc.admin.ServerConnectionManager;
import com.tc.config.schema.L2Info;
import com.tc.stats.statistics.CountStatistic;
import com.tc.util.concurrent.ThreadUtil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.ObjectName;

public class ClusterModel extends Server implements IClusterModel {
  private Server[]              m_clusterServers;
  private Server                m_activeServer;
  private ActiveLocator         m_activeLocator;
  private boolean               m_userDisconnecting;

  private static final Server[] EMPTY_SERVERS = {};

  public ClusterModel() {
    this(ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT, ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  public ClusterModel(final String host, final int jmxPort, final boolean autoConnect) {
    super(host, jmxPort, autoConnect);
  }

  private void setActiveServer(Server server, ServerConnectionManager scm) {
    String[] creds = ServerConnectionManager.getCachedCredentials(scm);
    if (creds != null) {
      m_connectManager.setCredentials(creds);
    }

    boolean autoConnect = isAutoConnect();
    m_connectManager.setAutoConnect(false);
    m_connectManager.setL2Info(new L2Info(scm.getL2Info()));
    resetBeanProxies();

    try {
      m_connectManager.setConnected(m_connectManager.testIsConnected());
      Server oldActiveServer = m_activeServer;
      m_activeServer = server;
      firePropertyChange(PROP_ACTIVE_SERVER, oldActiveServer, server);
    } catch (Exception e) {
      m_connectManager.setAutoConnect(autoConnect);
    }
  }

  public Server getActiveServer() {
    return m_activeServer;
  }

  public synchronized Server[] getClusterServers() {
    if (m_clusterServers == null) m_clusterServers = super.getClusterServers();
    return Arrays.asList(m_clusterServers).toArray(EMPTY_SERVERS);
  }

  protected void setReady(boolean ready) {
    if (ready == isReady()) return;
    if (ready) {
      m_clusterServers = getClusterServers();
      for (Server server : m_clusterServers) {
        server.addPropertyChangeListener(new ServerPropertyChangeListener(server));
      }
    }
    super.setReady(ready);
  }

  class ServerPropertyChangeListener implements PropertyChangeListener {
    private Server m_server;

    ServerPropertyChangeListener(Server server) {
      super();
      m_server = server;
    }

    public void propertyChange(PropertyChangeEvent evt) {
      fireServerStateChanged(m_server, evt);
    }
  }

  public void addServerStateListener(ServerStateListener listener) {
    m_listenerList.add(ServerStateListener.class, listener);
  }

  public void removeServerStateListener(ServerStateListener listener) {
    m_listenerList.remove(ServerStateListener.class, listener);
  }

  protected void fireServerStateChanged(Server server, PropertyChangeEvent pce) {
    Object[] listeners = m_listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ServerStateListener.class) {
        ((ServerStateListener) listeners[i + 1]).serverStateChanged(server, pce);
      }
    }
  }

  private class ActiveLocator extends Thread {
    private ServerConnectionManager scm  = null;
    private volatile boolean        stop = false;

    void setStopped() {
      stop = true;
    }

    private ServerConnectionManager getConnectionManager(L2Info l2Info) {
      if (scm == null) {
        scm = new ServerConnectionManager(l2Info, false, null);
      } else {
        scm.setL2Info(l2Info);
      }
      scm.setCredentials(m_connectManager.getCredentials());
      return scm;
    }

    public void run() {
      Server[] servers = getClusterServers();
      if (servers.length > 1) {
        while (true) {
          if (stop) return;
          for (Server server : servers) {
            L2Info l2Info = server.getL2Info();
            if (l2Info.matches(getL2Info())) {
              continue;
            }
            ThreadUtil.reallySleep(1000);
            if (stop) {
              if (scm != null) scm.tearDown();
              return;
            }
            scm = getConnectionManager(l2Info);
            try {
              if (scm.testIsConnected()) {
                if (scm.testIsActive()) {
                  m_activeLocator = null;
                  setActiveServer(server, scm);
                  scm.tearDown();
                  return;
                }
              }
            } catch (Exception e) {
              /**/
            }
          }
        }
      }
      m_activeLocator = null;
    }
  }

  public void cancelActiveLocator() {
    if (m_activeLocator != null) {
      m_activeLocator.setStopped();
      m_activeLocator = null;
    }
  }

  public void testStartActiveLocator() {
    if (m_activeLocator == null) {
      m_activeLocator = new ActiveLocator();
      m_activeLocator.start();
    }
  }

  public void handleStarting() {
    cancelActiveLocator();
    if (!m_connectManager.testIsActive()) {
      testStartActiveLocator();
    }
  }

  class ActiveWaiter implements Runnable {
    public void run() {
      while (true) {
        int count = m_clusterServers.length;
        if (count == 0) break;
        boolean anyStarted = false;
        for (int i = 0; i < count; i++) {
          Server server = m_clusterServers[i];
          final ServerConnectionManager scm = server.getConnectionManager();
          if (!scm.equals(m_connectManager) && scm.isActive()) {
            setActiveServer(server, scm);
            return;
          }
          if (scm.isStarted()) {
            anyStarted = true;
          }
        }
        if (!anyStarted) break;
        try {
          Thread.sleep(1000);
        } catch (Exception e) {/**/
        }
      }
    }
  }

  void waitForNewActive() {
    Thread waiter = new Thread(new ActiveWaiter());
    waiter.start();
  }

  boolean tryFindNewActive() {
    int serverCount = m_clusterServers.length;
    if (serverCount > 1) {
      for (int i = 0; i < serverCount; i++) {
        Server server = m_clusterServers[i];
        ServerConnectionManager scm = server.getConnectionManager();
        if (!scm.equals(m_connectManager)) {
          if (scm.isActive()) {
            setActiveServer(server, scm);
            return true;
          } else if (scm.isStarted()) {
            waitForNewActive();
            return true;
          }
        }
      }
    }
    return false;
  }

  public void disconnect() {
    m_userDisconnecting = true;
    super.disconnect();
  }

  public void handleDisconnect() {
    super.handleDisconnect();
    if (!m_userDisconnecting && tryFindNewActive()) { return; }
    setReady(false);
    m_userDisconnecting = false;
  }

  public synchronized Map<IClusterNode, String> takeThreadDump() {
    long requestMillis = System.currentTimeMillis();
    Map<IClusterNode, String> map = new HashMap<IClusterNode, String>();

    for (int i = 0; i < m_clusterServers.length; i++) {
      Server server = m_clusterServers[i];
      try {
        map.put(server, server.takeThreadDump(requestMillis));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    Iterator<DSOClient> clientIter = m_clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      try {
        map.put(client, client.takeThreadDump(requestMillis));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return map;
  }

  public Map<DSOClient, CountStatistic> getClientTransactionRates() throws Exception {
    Map<ObjectName, CountStatistic> map = getDSOBean().getClientTransactionRates();
    Map<DSOClient, CountStatistic> result = new HashMap<DSOClient, CountStatistic>();
    Iterator<DSOClient> clientIter = m_clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      result.put(client, map.get(client.getObjectName()));
    }
    return result;
  }

  public Map<Server, CountStatistic> getServerTransactionRates() throws Exception {
    Map<Server, CountStatistic> result = new HashMap<Server, CountStatistic>();
    for (Server server : getClusterServers()) {
      if (server.isReady()) {
        result.put(server, server.getTransactionRate());
      }
    }
    return result;
  }

  public Map<IClient, Map<String, Object>> getPrimaryClientStatistics() throws Exception {
    Map<ObjectName, Map> map = getDSOBean().getPrimaryClientStatistics();
    Map<IClient, Map<String, Object>> result = new HashMap<IClient, Map<String, Object>>();
    Iterator<DSOClient> clientIter = m_clients.iterator();
    while (clientIter.hasNext()) {
      DSOClient client = clientIter.next();
      result.put(client, map.get(client.getObjectName()));
    }
    return result;
  }

  public Map<IServer, Map<String, Object>> getPrimaryServerStatistics() throws Exception {
    Map<IServer, Map<String, Object>> result = new HashMap<IServer, Map<String, Object>>();
    for (Server server : getClusterServers()) {
      if (server.isReady()) {
        result.put(server, server.getPrimaryStatistics());
      }
    }
    return result;
  }

  public void tearDown() {
    super.tearDown();
    m_clusterServers = null;
  }
}
