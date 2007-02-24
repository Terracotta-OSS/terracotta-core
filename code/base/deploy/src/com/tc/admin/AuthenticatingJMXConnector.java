package com.tc.admin;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEventListener;

import java.io.IOException;
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
  private final JMXServiceURL    m_url;              // @NOTThreadSafe
  private Map                    m_conEnv;
  private JMXConnector           m_connector;
  private final EventMulticaster m_authObserver;
  private final EventMulticaster m_collapseObserver;
  private final EventMulticaster m_exceptionObserver;
  private boolean                m_authenticating;
  private boolean                m_securityEnabled;
  private final Object           m_error_lock;
  private Exception              m_error;

  public AuthenticatingJMXConnector(JMXServiceURL url, Map env) throws IOException {
    if (false) throw new IOException(); // quiet compiler
    (this.m_env = new HashMap()).putAll(env);
    (this.m_authEnv = new HashMap()).putAll(env);
    this.m_url = url;
    this.m_error_lock = new Object();
    this.m_authObserver = new EventMulticaster();
    this.m_collapseObserver = new EventMulticaster();
    this.m_exceptionObserver = new EventMulticaster();
  }

  private synchronized JMXConnector getConnector() {
    return m_connector;
  }

  private synchronized void setConnector(JMXConnector connector) {
    this.m_connector = connector;
  }

  public void addAuthenticationListener(UpdateEventListener listener) {
    m_authObserver.addListener(listener);
  }

  public void addCollapseListener(UpdateEventListener listener) {
    this.m_collapseObserver.addListener(listener);
  }

  public void addExceptionListener(UpdateEventListener listener) {
    this.m_exceptionObserver.addListener(listener);
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
        m_securityEnabled = true;
        try {
          Thread.sleep(500);
        } catch (InterruptedException ie) {
          m_collapseObserver.fireUpdateEvent();
          return;
        }
        m_authenticating = true;
        m_authObserver.fireUpdateEvent();
        try {
          while (m_authenticating)
            wait();
        } catch (InterruptedException ie) {
          m_exceptionObserver.fireUpdateEvent();
          return;
        }
      } else {
        throw e;
      }
    } catch (IOException e) {
      m_exceptionObserver.fireUpdateEvent();
      throw e;
    }
    throwExceptions();
  }

  private void throwExceptions() throws IOException {
    synchronized (m_error_lock) {
      if (m_error != null) {
        m_exceptionObserver.fireUpdateEvent();
        if (m_error instanceof IOException) throw (IOException) m_error;
        else if (m_error instanceof RuntimeException) throw (RuntimeException) m_error;
      }
    }
    if (m_securityEnabled) throw new AuthenticationException();
  }

  public synchronized void handleOkClick(String username, String password) {
    m_authEnv.put("jmx.remote.credentials", new String[] { username, password });
    try {
      m_collapseObserver.fireUpdateEvent();
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

  public static class AuthenticationException extends RuntimeException {
    public AuthenticationException() {
      super();
    }
  }
}
