/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.config.schema.L2Info;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.Timer;

public class ServerConnectionManager implements NotificationListener {
  private L2Info                  m_l2Info;
  private boolean                 m_autoConnect;
  private ConnectionContext       m_connectCntx;
  private ConnectionListener      m_connectListener;
  private JMXServiceURL           m_serviceURL;
  private HashMap                 m_connectEnv;
  private ServerHelper            m_serverHelper;
  private boolean                 m_connected;
  private boolean                 m_started;
  private boolean                 m_active;
  private Exception               m_connectException;
  private ConnectThread           m_connectThread;
  private ConnectionMonitorAction m_connectMonitorAction;
  private Timer                   m_connectMonitorTimer;

  private static final int        CONNECT_MONITOR_PERIOD = 1000;

  public ServerConnectionManager(String host, int port, boolean autoConnect, ConnectionListener listener) {
    this(new L2Info(host, host, port), autoConnect, listener);
  }

  public ServerConnectionManager(L2Info l2Info, boolean autoConnect, ConnectionListener listener) {
    m_autoConnect = autoConnect;
    m_connectListener = listener;
    m_serverHelper = ServerHelper.getHelper();

    setL2Info(l2Info);
  }

  private void setL2Info(L2Info l2Info) {
    cancelActiveServices();

    m_l2Info = l2Info;
    m_connectCntx = new ConnectionContext(l2Info);

    try {
      m_serviceURL = new JMXServiceURL(getSecureJMXServicePath());
      if (isAutoConnect()) {
        startConnect();
      }
    } catch (Exception e) {/**/
    }
  }

  public void setHostname(String hostname) {
    setL2Info(new L2Info(m_l2Info.name(), hostname, m_l2Info.jmxPort()));
  }

  public void setJMXPortNumber(int port) {
    setL2Info(new L2Info(m_l2Info.name(), m_l2Info.host(), port));
  }

  public void setAutoConnect(boolean autoConnect) {
    if ((m_autoConnect = autoConnect) == true) {
      if (!m_connected) {
        startConnect();
      }
    } else {
      cancelActiveServices();
    }
  }

  public boolean isAutoConnect() {
    return m_autoConnect;
  }

  public void setConnectionContext(ConnectionContext cc) {
    m_connectCntx = cc;
  }

  public ConnectionContext getConnectionContext() {
    return m_connectCntx;
  }

  public void setJMXConnector(JMXConnector jmxc) throws IOException {
    m_connectCntx.jmxc = jmxc;
    m_connectCntx.mbsc = jmxc.getMBeanServerConnection();
    setConnected(true);
  }

  protected void setConnected(boolean connected) {
    if (m_connected != connected) {
      m_connected = connected;
      if (m_connected == false) {
        cancelActiveServices();
        m_active = m_started = false;
        if (isAutoConnect()) {
          startConnect();
        }
      } else { // connected
        m_started = true;
        if ((m_active = internalIsActive()) == false) {
          addActivationListener();
        }
        initConnectionMonitor();
      }

      // Notify listener that the connection state changed.
      if (m_connectListener != null) {
        m_connectListener.handleConnection();
      }
    }
  }

  /**
   * Mark not-connected, notify, cancel connection monitor, don't startup auto-connect thread.
   */
  void disconnectOnExit() {
    cancelActiveServices();
    if (m_connected) {
      m_connected = false;
      if (m_connectListener != null) {
        m_connectListener.handleConnection();
      }
    }
  }

  /**
   * Since we have all of this infrastructure, turn off the JMXRemote connection monitoring stuff.
   */
  Map getConnectionEnvironment() {
    if (m_connectEnv == null) {
      m_connectEnv = new HashMap();
      //m_connectEnv.put("jmx.remote.x.server.connection.timeout", new Long(0));
      m_connectEnv.put("jmx.remote.x.client.connection.check.period", new Long(0));
      m_connectEnv.put(JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER, getClass().getClassLoader());
    }
    //m_connectEnv.put("jmx.remote.credentials", new String[] { getUsername(), getPassword() }); XXX: remove getUsername etc.
    return m_connectEnv;
  }

  private void initConnector() throws Exception {
    // TODO: implement this call correctly (this code is not complete)
    m_connectCntx.jmxc = new AuthenticatingJMXConnector(m_serviceURL, getConnectionEnvironment());
  }

  private void startConnect() {
    try {
      cancelConnectThread();
      initConnector();
      m_connectThread = new ConnectThread();
      m_connectThread.start();
    } catch (Exception e) {
      if (m_connectListener != null) {
        m_connectException = e;
        m_connectListener.handleException();
      }
    }
  }

  private void cancelConnectThread() {
    if (m_connectThread != null && m_connectThread.isAlive()) {
      try {
        m_connectThread.interrupt();
        m_connectThread = null;
      } catch (Exception ignore) {/**/
      }
    }
  }

  public boolean testIsConnected() throws Exception {
    if (m_connectCntx.jmxc == null) {
      initConnector();
    }
    m_connectCntx.jmxc.connect();
    m_connectCntx.mbsc = m_connectCntx.jmxc.getMBeanServerConnection();
    m_connectException = null;

    return true;
  }

  class ConnectThread extends Thread {
    ConnectThread() {
      super();
      setPriority(MIN_PRIORITY);
    }

    public void run() {
      while (!m_connected) {
        try {
          setConnected(testIsConnected());
          return;
        } catch (Exception e) {
          if (m_connectListener != null) {
            m_connectException = e;
            m_connectListener.handleException();
          }
        }

        try {
          sleep(2000);
        } catch (InterruptedException ie) {
          // We may interrupt the connect thread when a new host or port comes in
          // because we have to recreate the connection context, JMX service URL,
          // and connect thread.
          return;
        }
      }
    }
  }

  JMXServiceURL getJMXServiceURL() {
    return m_serviceURL;
  }
  
  private String getSecureJMXServicePath() {
    return "service:jmx:rmi:///jndi/rmi://" + this + "/jmxrmi";
  }

  public String getName() {
    return m_l2Info.name();
  }

  public String getHostname() {
    return m_l2Info.host();
  }

  public int getJMXPortNumber() {
    return m_l2Info.jmxPort();
  }

  public boolean isConnected() {
    return m_connected;
  }

  public Exception getConnectionException() {
    return m_connectException;
  }

  public boolean isActive() {
    return m_active;
  }

  private boolean internalIsActive() {
    try {
      return m_serverHelper.isActive(m_connectCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isStarted() {
    return m_started;
  }

  void initConnectionMonitor() {
    if (m_connectMonitorAction == null) {
      m_connectMonitorAction = new ConnectionMonitorAction();
      m_connectMonitorTimer = new Timer(CONNECT_MONITOR_PERIOD, m_connectMonitorAction);
    }
    m_connectMonitorTimer.start();
  }

  private class ConnectionMonitorAction implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      if (m_connectCntx.isConnected()) {
        try {
          m_connectCntx.testConnection();
        } catch (Exception e) {
          setConnected(false);
        }
      }
    }

    void stop() {
      if (m_connectMonitorTimer != null && m_connectMonitorTimer.isRunning()) {
        m_connectMonitorTimer.stop();
      }
    }
  }

  void cancelConnectionMonitor() {
    if (m_connectMonitorAction != null) {
      m_connectMonitorAction.stop();
    }
  }

  /**
   * Register for a JMX callback when the server transitions from started->active. We do this when we notice that the
   * server is started but not yet active.
   */
  void addActivationListener() {
    try {
      ObjectName infoMBean = m_serverHelper.getServerInfoMBean(m_connectCntx);
      m_connectCntx.addNotificationListener(infoMBean, this);
      if ((m_active = internalIsActive()) == true) {
        m_connectCntx.removeNotificationListener(infoMBean, this);
      }
    } catch (Exception e) {/**/
    }
  }

  void removeActivationListener() {
    try {
      ObjectName infoMBean = m_serverHelper.getServerInfoMBean(m_connectCntx);
      m_connectCntx.removeNotificationListener(infoMBean, this);
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
        m_active = true;
        removeActivationListener();
        if (m_connectListener != null) {
          m_connectListener.handleConnection();
        }
      }
    }
  }

  public String toString() {
    return getHostname() + ":" + getJMXPortNumber();
  }

  void cancelActiveServices() {
    cancelConnectThread();
    cancelConnectionMonitor();

    if (m_started && !m_active) {
      removeActivationListener();
    }
    if (m_connectCntx != null) {
      m_connectCntx.reset();
    }
  }

  void tearDown() {
    cancelActiveServices();

    m_l2Info = null;
    m_serverHelper = null;
    m_connectCntx = null;
    m_connectListener = null;
    m_serviceURL = null;
    m_connectThread = null;
  }
}
