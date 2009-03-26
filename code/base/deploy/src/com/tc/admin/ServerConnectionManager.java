/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.config.schema.L2Info;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

public class ServerConnectionManager implements NotificationListener {
  private L2Info                             l2Info;
  private boolean                            autoConnect;
  private ConnectionContext                  connectCntx;
  private ConnectionListener                 connectListener;
  private JMXConnectorProxy                  jmxConnector;
  private HashMap<String, Object>            connectEnv;
  private ServerHelper                       serverHelper;
  private volatile boolean                   connected;
  private volatile boolean                   started;
  private volatile boolean                   active;
  private volatile boolean                   passiveUninitialized;
  private volatile boolean                   passiveStandby;
  private Exception                          connectException;
  private ConnectThread                      connectThread;
  private ConnectionMonitorAction            connectMonitorAction;
  private Timer                              connectMonitorTimer;

  private static final Map<String, String[]> credentialsMap         = new HashMap<String, String[]>();

  private static final int                   CONNECT_MONITOR_PERIOD = 1000;

  private static final Object                connectTestLock        = new Object();

  static {
    if (!Boolean.getBoolean("javax.management.remote.debug")) {
      String levelName = System.getProperty("ServerConnectionManager.logLevel");
      Level level = Level.OFF;

      if (levelName != null) {
        try {
          level = Level.parse(levelName);
        } catch (IllegalArgumentException ie) {
          level = Level.ALL;
        }
      }
      Logger.getLogger("javax.management.remote").setLevel(level);
    }
  }

  public ServerConnectionManager(String host, int port, boolean autoConnect, ConnectionListener listener) {
    this(new L2Info(host, host, port), autoConnect, listener);
  }

  public ServerConnectionManager(L2Info l2Info, boolean autoConnect, ConnectionListener listener) {
    this.autoConnect = autoConnect;
    connectListener = listener;
    serverHelper = ServerHelper.getHelper();

    setL2Info(l2Info);
  }

  public void setConnectionListener(ConnectionListener listener) {
    connectListener = listener;
  }

  public ConnectionListener getConnectionListener() {
    return connectListener;
  }

  public L2Info getL2Info() {
    return l2Info;
  }

  private synchronized void resetConnectedState() {
    active = started = passiveUninitialized = passiveStandby = false;
  }

  private void resetAllState() {
    _setConnected(false);
    resetConnectedState();
  }

  public void setL2Info(L2Info l2Info) {
    cancelActiveServices();
    resetAllState();

    this.l2Info = l2Info;
    connectCntx = new ConnectionContext(l2Info);

    try {
      if (isAutoConnect()) {
        startConnect();
      }
    } catch (Exception e) {/**/
    }
  }

  public void setHostname(String hostname) {
    setL2Info(new L2Info(l2Info.name(), hostname, l2Info.jmxPort()));
  }

  public void setJMXPortNumber(int port) {
    setL2Info(new L2Info(l2Info.name(), l2Info.host(), port));
  }

  public void setCredentials(String username, String password) {
    setCredentials(new String[] { username, password });
  }

  public void setCredentials(String[] creds) {
    Map<String, Object> connEnv = getConnectionEnvironment();
    connEnv.put("jmx.remote.credentials", creds);
  }

  public String[] getCredentials() {
    Map<String, Object> connEnv = getConnectionEnvironment();
    return (String[]) connEnv.get("jmx.remote.credentials");
  }

  static void cacheCredentials(ServerConnectionManager scm, String[] credentials) {
    credentialsMap.put(scm.toString(), credentials);
    credentialsMap.put(scm.safeGetHostName(), credentials);
  }

  public static String[] getCachedCredentials(ServerConnectionManager scm) {
    String[] result = credentialsMap.get(scm.toString());
    if (result == null) {
      result = credentialsMap.get(scm.safeGetHostName());
    }
    return result;
  }

  public void setAutoConnect(boolean autoConnect) {
    if (this.autoConnect != autoConnect) {
      if ((this.autoConnect = autoConnect) == true) {
        if (!connected) {
          startConnect();
        }
      } else {
        cancelActiveServices();
      }
    }
  }

  public boolean isAutoConnect() {
    return autoConnect;
  }

  public void setConnectionContext(ConnectionContext cc) {
    connectCntx = cc;
  }

  public ConnectionContext getConnectionContext() {
    return connectCntx;
  }

  public void setJMXConnector(JMXConnector jmxc) throws IOException {
    connectException = null;
    connectCntx.jmxc = jmxc;
    connectCntx.mbsc = jmxc.getMBeanServerConnection();
    setConnected(true);
  }

  public void setConnected(boolean connected) {
    if (isConnected() != connected) {
      synchronized (this) {
        _setConnected(connected);
        if (isConnected() == false) {
          cancelActiveServices();
          resetConnectedState();
        } else {
          cacheCredentials(ServerConnectionManager.this, getCredentials());
          started = true;
          if ((active = internalIsActive()) == false) {
            if ((passiveUninitialized = internalIsPassiveUninitialized()) == false) {
              passiveStandby = internalIsPassiveStandby();
            }
            addActivationListener();
          }
        }
      }

      // Notify listener that the connection state changed.
      if (connectListener != null) {
        connectListener.handleConnection();
      }

      if (connected) {
        initConnectionMonitor();
      } else if (isAutoConnect()) {
        startConnect();
      }
    }
  }

  /**
   * Mark not-connected, notify, cancel connection monitor, don't startup auto-connect thread.
   */
  public void disconnect() {
    cancelActiveServices();
    _setConnected(false);
    if (connectListener != null) {
      connectListener.handleConnection();
    }
  }

  /**
   * Since we have all of this infrastructure, turn off the JMXRemote connection monitoring stuff.
   */
  public Map<String, Object> getConnectionEnvironment() {
    if (connectEnv == null) {
      connectEnv = new HashMap<String, Object>();
      connectEnv.put("jmx.remote.x.client.connection.check.period", Integer.valueOf(0));
      connectEnv.put("jmx.remote.default.class.loader", getClass().getClassLoader());
    }
    return connectEnv;
  }

  private void initConnector() {
    if (jmxConnector != null) {
      try {
        jmxConnector.close();
      } catch (Exception e) {/**/
      }
    }
    jmxConnector = new JMXConnectorProxy(getHostname(), getJMXPortNumber(), getConnectionEnvironment());
  }

  public JMXConnector getJmxConnector() {
    initConnector();
    return jmxConnector;
  }

  private void startConnect() {
    if (serverHelper == null) return;
    try {
      cancelConnectThread();
      initConnector();
      connectThread = new ConnectThread();
      connectThread.start();
    } catch (Exception e) {
      connectException = e;
      if (connectListener != null) {
        connectListener.handleException();
      }
    }
  }

  private void cancelConnectThread() {
    if (connectThread != null && connectThread.isAlive()) {
      try {
        connectThread.cancel();
        connectThread = null;
      } catch (Exception ignore) {/**/
      }
    }
  }

  static boolean isConnectException(IOException ioe) {
    Throwable t = ioe;

    while (t != null) {
      if (t instanceof ConnectException) { return true; }
      t = t.getCause();
    }

    return false;
  }

  public boolean testIsConnected() throws Exception {
    if (connectCntx == null) return false;

    synchronized (connectTestLock) {
      if (connectCntx == null) return false;

      if (connectCntx.jmxc == null) {
        initConnector();
      }

      jmxConnector.connect(getConnectionEnvironment());
      connectCntx.mbsc = jmxConnector.getMBeanServerConnection();
      connectCntx.jmxc = jmxConnector;
      connectException = null;

      return true;
    }
  }

  class ConnectThread extends Thread {
    private boolean cancel = false;

    ConnectThread() {
      super();
      setPriority(MIN_PRIORITY);
    }

    @Override
    public void run() {
      while (!cancel && !connected) {
        try {
          boolean isConnected = testIsConnected();
          if (!cancel) {
            setConnected(isConnected);
          }
          return;
        } catch (Exception e) {
          if (cancel) {
            return;
          } else {
            connectException = e;
            if (connectListener != null) {
              connectListener.handleException();
            }
          }
        }

        try {
          sleep(1000);
        } catch (InterruptedException ie) {
          // We may interrupt the connect thread when a new host or port comes in
          // because we have to recreate the connection context, JMX service URL,
          // and connect thread.
          return;
        }
      }
    }

    void cancel() {
      cancel = true;
    }
  }

  synchronized JMXServiceURL getJMXServiceURL() {
    return jmxConnector.getServiceURL();
  }

  public synchronized String getName() {
    return l2Info.name();
  }

  public synchronized String getHostname() {
    return l2Info.host();
  }

  public synchronized InetAddress getInetAddress() throws UnknownHostException {
    return l2Info.getInetAddress();
  }

  public synchronized String getCanonicalHostName() throws UnknownHostException {
    return getInetAddress().getCanonicalHostName();
  }

  public synchronized String getHostAddress() throws UnknownHostException {
    return l2Info.getHostAddress();
  }

  public synchronized int getJMXPortNumber() {
    return l2Info.jmxPort();
  }

  private synchronized void _setConnected(boolean isConnected) {
    this.connected = isConnected;
  }

  public synchronized boolean isConnected() {
    return connected;
  }

  public synchronized Exception getConnectionException() {
    return connectException;
  }

  public synchronized boolean testIsActive() {
    return internalIsActive();
  }

  public synchronized boolean isActive() {
    return active;
  }

  public synchronized boolean canShutdown() {
    try {
      return serverHelper.canShutdown(connectCntx);
    } catch (Exception e) {
      return false;
    }
  }

  private synchronized boolean internalIsActive() {
    try {
      return serverHelper.isActive(connectCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public synchronized boolean isStarted() {
    return started;
  }

  private boolean internalIsPassiveUninitialized() {
    try {
      return serverHelper.isPassiveUninitialized(connectCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public synchronized boolean isPassiveUninitialized() {
    return passiveUninitialized;
  }

  private boolean internalIsPassiveStandby() {
    try {
      return serverHelper.isPassiveStandby(connectCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public synchronized boolean isPassiveStandby() {
    return passiveStandby;
  }

  synchronized void initConnectionMonitor() {
    if (connectMonitorAction == null) {
      connectMonitorAction = new ConnectionMonitorAction();
    }
    if (connectMonitorTimer == null) {
      connectMonitorTimer = new Timer();
      connectMonitorTimer.schedule(connectMonitorAction, CONNECT_MONITOR_PERIOD, CONNECT_MONITOR_PERIOD);
    }
  }

  private class ConnectionMonitorAction extends TimerTask {
    @Override
    public void run() {
      if (connectCntx != null && connectCntx.isConnected()) {
        try {
          connectCntx.testConnection();
        } catch (Exception e) {
          cancelConnectionMonitor();
          setConnected(false);
        }
      }
    }
  }

  synchronized void cancelConnectionMonitor() {
    if (connectMonitorTimer != null) {
      connectMonitorTimer.cancel();
      connectMonitorAction.cancel();
      connectMonitorAction = null;
      connectMonitorTimer = null;
    }
  }

  /**
   * Register for a JMX callback when the server transitions from started->...->active. We do this when we notice that
   * the server is started but not yet active.
   */
  void addActivationListener() {
    try {
      connectCntx.addNotificationListener(L2MBeanNames.TC_SERVER_INFO, this);
      if ((active = internalIsActive()) == true) {
        connectCntx.removeNotificationListener(L2MBeanNames.TC_SERVER_INFO, this);
      }
    } catch (Exception e) {/**/
    }
  }

  synchronized void removeActivationListener() {
    try {
      if (connectCntx != null) {
        connectCntx.removeNotificationListener(L2MBeanNames.TC_SERVER_INFO, this);
      }
    } catch (Exception e) {/**/
    }
  }

  /**
   * JMX callback notifying that the server has transitioned from started->active.
   */
  public void handleNotification(Notification notice, Object handback) {
    if (notice instanceof AttributeChangeNotification) {
      AttributeChangeNotification acn = (AttributeChangeNotification) notice;

      if (acn.getAttributeType().equals("jmx.terracotta.L2.active")) {
        active = true;
        removeActivationListener();
        if (connectListener != null) {
          connectListener.handleConnection();
        }
      } else if (acn.getAttributeType().equals("jmx.terracotta.L2.passive-uninitialized")) {
        passiveUninitialized = true;
        if (connectListener != null) {
          connectListener.handleConnection();
        }
      } else if (acn.getAttributeType().equals("jmx.terracotta.L2.passive-standby")) {
        passiveUninitialized = false;
        passiveStandby = true;
        if (connectListener != null) {
          connectListener.handleConnection();
        }
      }
    }
  }

  public String safeGetHostName() {
    try {
      String s = getCanonicalHostName();
      return s;
    } catch (UnknownHostException uhe) {
      return getHostname();
    }
  }

  public String safeGetHostAddress() {
    try {
      String s = getHostAddress();
      return s;
    } catch (UnknownHostException uhe) {
      return "<unknown>";
    }
  }

  @Override
  public String toString() {
    return safeGetHostName() + ":" + getJMXPortNumber();
  }

  public String getStatusString() {
    StringBuffer sb = new StringBuffer();
    if (connected) {
      if (active) sb.append("Active-coordinator");
      else if (passiveUninitialized) sb.append("Passive-uninitialized");
      else if (passiveStandby) sb.append("Passive-standby");
      else if (started) sb.append("Started");
      else sb.append("unknown");
    } else {
      sb.append("Not connected");
    }
    return sb.toString();
  }

  public void dump(String prefix) {
    System.out.println(prefix + this + ":connected=" + connected + ",autoConnect=" + autoConnect + ",started="
                       + started + ",exception=" + connectException);
  }

  void cancelActiveServices() {
    cancelConnectThread();
    cancelConnectionMonitor();

    if (started) {
      removeActivationListener();
    }
    if (connectCntx != null) {
      connectCntx.reset();
    }
  }

  public void tearDown() {
    cancelActiveServices();

    synchronized (this) {
      serverHelper = null;
      connectCntx = null;
      connectListener = null;
      connectThread = null;
    }
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(getJMXPortNumber()).append(safeGetHostName()).toHashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ServerConnectionManager)) return false;

    ServerConnectionManager other = (ServerConnectionManager) o;
    String otherHostname = other.safeGetHostName();
    int otherJMXPort = other.getJMXPortNumber();
    String hostname = safeGetHostName();
    int jmxPort = getJMXPortNumber();

    return otherJMXPort == jmxPort && StringUtils.equals(otherHostname, hostname);
  }
}
