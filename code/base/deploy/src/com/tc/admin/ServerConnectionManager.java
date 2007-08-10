/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.config.schema.L2Info;

import java.io.IOException;
import java.rmi.ConnectException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ServerConnectionManager implements NotificationListener {
  private L2Info                  m_l2Info;
  private boolean                 m_autoConnect;
  private ConnectionContext       m_connectCntx;
  private ConnectionListener      m_connectListener;
  private JMXServiceURL           m_serviceURL;
  private JMXServiceURL           m_secureServiceURL;
  private JMXConnector            m_jmxConnector;
  private JMXConnector            m_secureJmxConnector;
  private HashMap                 m_connectEnv;
  private ServerHelper            m_serverHelper;
  private boolean                 m_connected;
  private boolean                 m_started;
  private boolean                 m_active;
  private boolean                 m_passiveUninitialized;
  private boolean                 m_passiveStandby;
  private Exception               m_connectException;
  private ConnectThread           m_connectThread;
  private ConnectionMonitorAction m_connectMonitorAction;
  private Timer                   m_connectMonitorTimer;
  private AutoConnectListener     m_autoConnectListener;

  private static final Map        m_credentialsMap = new HashMap();
  
  private static final int        CONNECT_MONITOR_PERIOD = 1000;

  private static final Object     m_connectTestLock = new Object();
  
  static {
    String levelName = System.getProperty("ServerConnectionManager.logLevel");
    Level level = Level.OFF;

    if(levelName != null) {
      try {
        level = Level.parse(levelName);
      } catch(IllegalArgumentException ie) {
        level = Level.ALL;
      }
    }
    Logger.getLogger("javax.management.remote.rmi").setLevel(level);
  }

  public ServerConnectionManager(String host, int port, boolean autoConnect, ConnectionListener listener) {
    this(new L2Info(host, host, port), autoConnect, listener);
  }

  public ServerConnectionManager(L2Info l2Info, boolean autoConnect, ConnectionListener listener) {
    m_autoConnect = autoConnect;
    m_connectListener = listener;
    m_serverHelper = ServerHelper.getHelper();

    setL2Info(l2Info);
  }

  public L2Info getL2Info() {
    return m_l2Info;
  }
  
  public void setL2Info(L2Info l2Info) {
    cancelActiveServices();

    m_l2Info = l2Info;
    m_connectCntx = new ConnectionContext(l2Info);

    try {
      m_secureServiceURL = new JMXServiceURL(getSecureJMXServicePath());
      m_serviceURL = new JMXServiceURL(getJMXServicePath());
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

  public void setCredentials(String username, String password) {
    Map connEnv = getConnectionEnvironment();
    connEnv.put("jmx.remote.credentials", new String[] { username, password });
  }
  
  public String[] getCredentials() {
    Map connEnv = getConnectionEnvironment();
    return (String[])connEnv.get("jmx.remote.credentials");
  }

  static void cacheCredentials(ServerConnectionManager scm, String[] credentials) {
    m_credentialsMap.put(scm.toString(), credentials);
    m_credentialsMap.put(scm.getHostname(), credentials);
  }
  
  public static String[] getCachedCredentials(ServerConnectionManager scm) {
    String[] result = (String[])m_credentialsMap.get(scm.toString());
    if(result == null) {
      result = (String[])m_credentialsMap.get(scm.getHostname());
    }
    return result;
  }
  
  public void setAutoConnect(boolean autoConnect) {
    if (m_autoConnect != autoConnect) {
      if ((m_autoConnect = autoConnect) == true) {
        if (!m_connected) {
          startConnect();
        }
      } else {
        cancelActiveServices();
      }
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
    m_connectException = null;
    m_connectCntx.jmxc = jmxc;
    m_connectCntx.mbsc = jmxc.getMBeanServerConnection();
    setConnected(true);
  }

  protected synchronized void setConnected(boolean connected) {
    if (m_connected != connected) {
      m_connected = connected;
      if (m_connected == false) {
        cancelActiveServices();
        m_active = m_started = m_passiveUninitialized = m_passiveStandby = false;
        if (isAutoConnect()) {
          startConnect();
        }
      } else { // connected
        cacheCredentials(ServerConnectionManager.this, getCredentials());
        m_started = true;
        if ((m_active = internalIsActive()) == false) {
          if((m_passiveUninitialized = internalIsPassiveUninitialized()) == false) {
            m_passiveStandby = internalIsPassiveStandby();
          }
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
  public Map getConnectionEnvironment() {
    if (m_connectEnv == null) {
      m_connectEnv = new HashMap();
      m_connectEnv.put("jmx.remote.x.client.connection.check.period", new Long(0));
      m_connectEnv.put("jmx.remote.default.class.loader", getClass().getClassLoader());
    }
    return m_connectEnv;
  }

  class ConnectorCloser implements Runnable {
    private JMXConnector m_connector;
    
    ConnectorCloser(JMXConnector connector) {
      m_connector = connector;
    }
    
    public void run() {
      try {
        m_connector.close();
      } catch(Exception e) {/**/}
    }
  }
  
  private void initConnectors() throws IOException {
    if(m_secureJmxConnector != null) new Thread(new ConnectorCloser(m_secureJmxConnector)).start();
    m_secureJmxConnector = JMXConnectorFactory.newJMXConnector(m_secureServiceURL, getConnectionEnvironment());
    
    if(m_jmxConnector != null) new Thread(new ConnectorCloser(m_jmxConnector)).start();
    m_jmxConnector = JMXConnectorFactory.newJMXConnector(m_serviceURL, getConnectionEnvironment());
  }

  public JMXConnector getJmxConnector() throws IOException {
    initConnectors();
    return m_jmxConnector;
  }
  
  public JMXConnector getSecureJmxConnector() throws IOException {
    initConnectors();
    return m_secureJmxConnector;
  }
  
  private void startConnect() {
    try {
      cancelConnectThread();
      initConnectors();
      m_connectThread = new ConnectThread();
      m_connectThread.start();
    } catch (Exception e) {
      m_connectException = e;
      if (m_connectListener != null) {
        m_connectListener.handleException();
      }
    }
  }

  private void cancelConnectThread() {
    if (m_connectThread != null && m_connectThread.isAlive()) {
      try {
        m_connectThread.cancel();
        m_connectThread = null;
      } catch (Exception ignore) {/**/
      }
    }
  }

  static boolean isConnectException(IOException ioe) {
    Throwable t = ioe;
    
    while(t != null) {
      if(t instanceof ConnectException) {
        return true;
      }
      t = t.getCause();
    }
    
    return false;
  }
  
  public boolean testIsConnected() throws Exception {
    synchronized(m_connectTestLock) {
      if (m_connectCntx.jmxc == null) {
        initConnectors();
      }
      
      JMXConnector connector = null;
      try {
        m_secureJmxConnector.connect(getConnectionEnvironment());
        connector = m_secureJmxConnector;
      } catch(IOException ioe) {
        if(isConnectException(ioe)) {
          throw ioe;
        }
        m_jmxConnector.connect(getConnectionEnvironment());
        connector = m_jmxConnector;
      }
      m_connectCntx.mbsc = connector.getMBeanServerConnection();
      m_connectCntx.jmxc = connector;
      m_connectException = null;
  
      return true;
    }
  }

  class ConnectThread extends Thread {
    private boolean m_cancel = false;
    
    ConnectThread() {
      super();
      setPriority(MIN_PRIORITY);
    }

    public void run() {
      try {
        sleep(500);
      } catch (InterruptedException ie) {/**/}
      
      while (!m_cancel && !m_connected) {
        try {
          boolean isConnected = testIsConnected();
          if(!m_cancel) {
            setConnected(isConnected);
          }
          return;
        } catch (Exception e) {
          if(m_cancel) {
            return;
          } else {
            m_connectException = e;
            if (m_connectListener != null) {
              if (e instanceof SecurityException) {
                setAutoConnect(false);
                fireToggleAutoConnectEvent();
                m_connectListener.handleException();
                return;
              }
              m_connectListener.handleException();
            }
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

    void cancel() {
      m_cancel = true;
    }
  }

  void addToggleAutoConnectListener(AutoConnectListener listener) {
    m_autoConnectListener = listener;
  }

  private void fireToggleAutoConnectEvent() {
    if (m_autoConnectListener != null) m_autoConnectListener.handleEvent();
  }

  JMXServiceURL getJMXServiceURL() {
    return m_serviceURL;
  }

  JMXServiceURL getSecureJMXServiceURL() {
    return m_secureServiceURL;
  }

  private String getJMXServicePath() {
    return "service:jmx:jmxmp://" + this;
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

  private boolean internalIsPassiveUninitialized() {
    try {
      return m_serverHelper.isPassiveUninitialized(m_connectCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isPassiveUninitialized() {
    return m_passiveUninitialized;
  }

  private boolean internalIsPassiveStandby() {
    try {
      return m_serverHelper.isPassiveStandby(m_connectCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isPassiveStandby() {
    return m_passiveStandby;
  }
    
  void initConnectionMonitor() {
    if (m_connectMonitorAction == null) {
      m_connectMonitorAction = new ConnectionMonitorAction();
    }
    if (m_connectMonitorTimer == null) {
      m_connectMonitorTimer = new Timer();
      m_connectMonitorTimer.schedule(m_connectMonitorAction, CONNECT_MONITOR_PERIOD, CONNECT_MONITOR_PERIOD);
    }
   }

  private class ConnectionMonitorAction extends TimerTask {
    public void run() {
      if (m_connectCntx.isConnected()) {
        try {
          m_connectCntx.testConnection();
        } catch (Exception e) {
          cancelConnectionMonitor();
          setConnected(false);
        }
      }
    }
  }

  void cancelConnectionMonitor() {
    if (m_connectMonitorTimer != null) {
      m_connectMonitorTimer.cancel();
      m_connectMonitorAction.cancel();
      m_connectMonitorAction = null;
      m_connectMonitorTimer = null;
    }
  }

  /**
   * Register for a JMX callback when the server transitions from started->...->active.
   * We do this when we notice that the server is started but not yet active.
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
      } else if (acn.getAttributeType().equals("jmx.terracotta.L2.passive-uninitialized")) {
        m_passiveUninitialized = true;
        if (m_connectListener != null) {
          m_connectListener.handleConnection();
        }
      } else if (acn.getAttributeType().equals("jmx.terracotta.L2.passive-standby")) {
        m_passiveStandby = true;
        if (m_connectListener != null) {
          m_connectListener.handleConnection();
        }
      }
    }
  }

  public String toString() {
    return getHostname() + ":" + getJMXPortNumber();
  }

  public void dump(String prefix) {
    System.out.println(prefix+this+",connected="+m_connected+",autoConnect="+m_autoConnect+",started="+m_started+",exception="+m_connectException);
  }
  
  void cancelActiveServices() {
    cancelConnectThread();
    cancelConnectionMonitor();

    if (m_started) {
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

  // --------------------------------------------------------------------------------

  public static interface AutoConnectListener extends EventListener {
    void handleEvent();
  }
}
