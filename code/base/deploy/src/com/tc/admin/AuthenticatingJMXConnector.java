package com.tc.admin;

import java.io.IOException;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

/**
 * Decorator for <tt>JMXConnector</tt>
 */
public final class AuthenticatingJMXConnector implements JMXConnector {

  private final Map              m_env;
  private final Map              m_authEnv;
  private final JMXServiceURL    m_url;                      // @NOTThreadSafe
  private Map                    m_conEnv;
  private JMXConnector           m_connector;
  private AuthenticationListener m_authListener;
  private AuthenticationListener m_collapseListener;
  private AuthenticationListener m_exceptionListener;
  private boolean                m_authenticating;
  private final Object           m_error_lock = new Object();
  private Exception              m_error;

  public AuthenticatingJMXConnector(JMXServiceURL url, Map env) throws IOException {
    if (false) throw new IOException(); // quiet compiler
    (this.m_env = new HashMap()).putAll(env);
    (this.m_authEnv = new HashMap()).putAll(env);
    this.m_url = url;
  }

  private synchronized JMXConnector getConnector() {
    return m_connector;
  }

  private synchronized void setConnector(JMXConnector connector) {
    this.m_connector = connector;
  }

  private void fireAuthenticationEvent() {
    if (m_authListener != null) m_authListener.handleEvent();
  }

  // does not support multicast
  public void addAuthenticationListener(AuthenticationListener listener) {
    this.m_authListener = listener;
  }

  private void fireCollapseEvent() {
    if (m_collapseListener != null) m_collapseListener.handleEvent();
  }

  // does not support multicast
  public void addCollapseListener(AuthenticationListener listener) {
    this.m_collapseListener = listener;
  }

  private void fireExceptionEvent() {
    if (m_exceptionListener != null) m_exceptionListener.handleEvent();
  }

  // does not support multicast
  public void addExceptionListener(AuthenticationListener listener) {
    this.m_exceptionListener = listener;
  }

  public void addConnectionNotificationListener(NotificationListener arg0, NotificationFilter arg1, Object arg2) {
    getConnector().addConnectionNotificationListener(arg0, arg1, arg2);
  }

  public void close() throws IOException {
    getConnector().close();
  }

  public void connect() throws IOException {
    getConnector().connect();
  }

  public synchronized void connect(Map conEnv) throws IOException {
    try {
      m_conEnv = conEnv;
      setConnector(JMXConnectorFactory.newJMXConnector(m_url, m_env));
      getConnector().connect(m_conEnv);
    } catch (RuntimeException e) {
      if (e instanceof SecurityException) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException ie) {
          fireCollapseEvent();
          return;
        }
        m_authenticating = true;
        fireAuthenticationEvent();
        try {
          while (m_authenticating)
            wait();
        } catch (InterruptedException ie) {
          fireExceptionEvent();
          return;
        }
      } else {
        throw e;
      }
    } catch (IOException e) {
      fireExceptionEvent();
      throw e;
    }
    throwExceptions();
  }

  private void throwExceptions() throws IOException {
    synchronized (m_error_lock) {
      if (m_error != null) {
        fireExceptionEvent();
        if (m_error instanceof IOException) throw (IOException) m_error;
        else if (m_error instanceof RuntimeException) throw (RuntimeException) m_error;
      }
    }
  }

  public synchronized void handleOkClick(String username, String password) {
    m_authEnv.put("jmx.remote.credentials", new String[] { username, password });
    try {
      fireCollapseEvent();
      setConnector(JMXConnectorFactory.newJMXConnector(m_url, m_authEnv));
      getConnector().connect(m_conEnv);
    } catch (Exception e) {
      synchronized (m_error_lock) {
        m_error = e;
      }
    }
    m_authenticating = false;
    notifyAll();
  }

  public String getConnectionId() throws IOException {
    return getConnector().getConnectionId();
  }

  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    return getConnector().getMBeanServerConnection();
  }

  public MBeanServerConnection getMBeanServerConnection(Subject arg0) throws IOException {
    return getConnector().getMBeanServerConnection(arg0);
  }

  public void removeConnectionNotificationListener(NotificationListener arg0, NotificationFilter arg1, Object arg2)
      throws ListenerNotFoundException {
    getConnector().removeConnectionNotificationListener(arg0, arg1, arg2);
  }

  public void removeConnectionNotificationListener(NotificationListener arg0) throws ListenerNotFoundException {
    getConnector().removeConnectionNotificationListener(arg0);
  }

  // --------------------------------------------------------------------------------

  public static interface AuthenticationListener extends EventListener {
    void handleEvent();
  }
}
