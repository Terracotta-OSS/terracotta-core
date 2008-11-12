/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.ConnectionContext;
import com.tc.admin.ServerConnectionManager;
import com.tc.config.schema.L2Info;
import com.tc.util.concurrent.ThreadUtil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.ObjectName;

public class ClusterModel extends Server implements IClusterModel {
  private Server[]               m_clusterServers;
  private PropertyChangeListener m_serverPropertyChangeListener;
  private Server                 m_activeServer;
  private ActiveLocator          m_activeLocator;
  private boolean                m_userDisconnecting;

  public ClusterModel() {
    this(ConnectionContext.DEFAULT_HOST, ConnectionContext.DEFAULT_PORT, ConnectionContext.DEFAULT_AUTO_CONNECT);
  }

  public ClusterModel(final String host, final int jmxPort, final boolean autoConnect) {
    super(host, jmxPort, autoConnect);
    m_serverPropertyChangeListener = new ServerPropertyChangeListener();
  }

  public void setConnectionCredentials(String[] creds) {
    super.setConnectionCredentials(creds);
    if (isReady()) {
      for (IServer server : getClusterServers()) {
        server.setConnectionCredentials(creds);
      }
    }
  }

  private void setActiveServer(Server server, ServerConnectionManager scm) {
    String[] creds = ServerConnectionManager.getCachedCredentials(scm);
    if (creds != null) {
      m_connectManager.setCredentials(creds);
    }

    boolean autoConnect = isAutoConnect();
    m_connectManager.setAutoConnect(false);
    m_connectManager.setL2Info(new L2Info(scm.getL2Info()));
    m_displayLabel = m_connectManager.toString();
    
    reset();

    try {
      m_connectManager.setConnected(m_connectManager.testIsConnected());
      Server oldActiveServer;
      synchronized (this) {
        oldActiveServer = m_activeServer;
        m_activeServer = server;
      }
      firePropertyChange(PROP_ACTIVE_SERVER, oldActiveServer, server);
    } catch (Exception e) {
      /**/
    } finally {
      m_connectManager.setAutoConnect(autoConnect);
    }
  }

  private void clearActiveServer() {
    Server oldActiveServer;
    synchronized (this) {
      oldActiveServer = m_activeServer;
      m_activeServer = null;
      if (m_clusterServers != null) {
        for (Server server : m_clusterServers = getClusterServers()) {
          server.addPropertyChangeListener(m_serverPropertyChangeListener);
          server.tearDown();
        }
        m_clusterServers = null;
      }
    }
    firePropertyChange(PROP_ACTIVE_SERVER, oldActiveServer, null);
  }

  public synchronized Server getActiveServer() {
    return m_activeServer;
  }

  public synchronized Server[] getClusterServers() {
    if (m_clusterServers == null) m_clusterServers = super.getClusterServers();
    return Arrays.asList(m_clusterServers).toArray(new Server[m_clusterServers.length]);
  }

  protected void setReady(boolean ready) {
    if(ready == isReady()) return;
    
    if (ready) {
      for (Server server : getClusterServers()) {
        server.addPropertyChangeListener(m_serverPropertyChangeListener);
      }
    }
    super.setReady(ready);
  }

  class ServerPropertyChangeListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      fireServerStateChanged((Server) evt.getSource(), evt);
    }
  }

  protected void setConnected(boolean connected) {
    super.setConnected(connected);
    if (isConnected()) {
      if (!m_connectManager.testIsActive()) {
        testStartActiveLocator();
      } else {
        testCancelActiveLocator();
      }
    }
  }

  public synchronized void addServerStateListener(ServerStateListener listener) {
    m_listenerList.add(ServerStateListener.class, listener);
  }

  public synchronized void removeServerStateListener(ServerStateListener listener) {
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

    private ActiveLocator() {
      super("ActiveServerLocator");
    }

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
          if (stop) {
            m_activeLocator = null;
            return;
          }
          for (Server server : servers) {
            L2Info l2Info = server.getL2Info();
            if (l2Info.matches(getL2Info())) {
              continue;
            }
            ThreadUtil.reallySleep(1000);
            if (stop) {
              m_activeLocator = null;
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

  private void testCancelActiveLocator() {
    if (m_activeLocator != null) {
      m_activeLocator.setStopped();
      while (m_activeLocator != null && m_activeLocator.isAlive()) {
        try {
          m_activeLocator.join();
        } catch (InterruptedException ie) {/**/
        }
      }
      m_activeLocator = null;
    }
  }

  private synchronized void testStartActiveLocator() {
    if (m_activeLocator == null) {
      m_activeLocator = new ActiveLocator();
      m_activeLocator.start();
    }
  }

  class ActiveWaiter implements Runnable {
    public void run() {
      while (true) {
        boolean anyStarted = false;
        synchronized (ClusterModel.this) {
          for (int i = 0; i < m_clusterServers.length; i++) {
            Server server = m_clusterServers[i];
            final ServerConnectionManager scm = server.getConnectionManager();
            if (scm.testIsActive()) {
              setActiveServer(server, scm);
              return;
            } else if (scm.isStarted()) {
              anyStarted = true;
            }
          }
        }
        if (!anyStarted) {
          clearActiveServer();
          break;
        }
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
    synchronized (ClusterModel.this) {
      if(m_clusterServers == null) return false;
      int serverCount = m_clusterServers.length;
      if (serverCount > 1) {
        for (int i = 0; i < serverCount; i++) {
          Server server = m_clusterServers[i];
          ServerConnectionManager scm = server.getConnectionManager();
          if (scm.testIsActive()) {
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
    clearActiveServer();
    m_userDisconnecting = false;
  }

  private static Future<String> threadDumpFuture(ExecutorService pool, final IClusterNode node, final long time) {
    return pool.submit(new Callable<String>() {
      public String call() throws Exception {
        return node.isReady() ? node.takeThreadDump(time) : "";
      }
    });
  }
  
  public synchronized Map<IClusterNode, Future<String>> takeThreadDump() {
    ExecutorService pool = Executors.newCachedThreadPool();
    long requestMillis = System.currentTimeMillis();
    Map<IClusterNode, Future<String>> map = new HashMap<IClusterNode, Future<String>>();

    for (Server server : getClusterServers()) {
      map.put(server, threadDumpFuture(pool, server, requestMillis));
    }
    for (DSOClient client : getClients()) {
      map.put(client, threadDumpFuture(pool, client, requestMillis));
    }
    pool.shutdown();
    
    return map;
  }

  public Map<DSOClient, Long> getClientTransactionRates() {
    Map<ObjectName, Long> map = getDSOBean().getClientTransactionRates();
    Map<DSOClient, Long> result = new HashMap<DSOClient, Long>();
    for (DSOClient client : getClients()) {
      result.put(client, map.get(client.getObjectName()));
    }
    return result;
  }

  public Map<Server, Number> getServerTransactionRates() {
    Map<Server, Number> result = new HashMap<Server, Number>();
    for (Server server : getClusterServers()) {
      if (server.isReady()) {
        result.put(server, server.getTransactionRate());
      }
    }
    return result;
  }

  public Map<IClient, Map<String, Object>> getPrimaryClientStatistics() {
    Map<ObjectName, Map> map = getDSOBean().getPrimaryClientStatistics();
    Map<IClient, Map<String, Object>> result = new HashMap<IClient, Map<String, Object>>();
    for (DSOClient client : getClients()) {
      result.put(client, map.get(client.getObjectName()));
    }
    return result;
  }

  public Map<IServer, Map<String, Object>> getPrimaryServerStatistics() {
    Map<IServer, Map<String, Object>> result = new HashMap<IServer, Map<String, Object>>();
    for (Server server : getClusterServers()) {
      if (server.isReady()) {
        result.put(server, server.getPrimaryStatistics());
      }
    }
    return result;
  }

  public synchronized void tearDown() {
    super.tearDown();
    m_clusterServers = null;
    m_serverPropertyChangeListener = null;
  }
}
