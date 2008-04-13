package com.tc.admin;

import java.io.IOException;
import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

/**
 * TODO: remove this and just use ServerConnectionManager
 */
public final class AuthenticatingJMXConnector implements JMXConnector {

  private ServerConnectionManager m_connectManager;
  private JMXConnector            m_connector;

  public AuthenticatingJMXConnector(ServerConnectionManager connectManager) {
    m_connectManager = connectManager;
  }

  private JMXConnector getConnector() {
    return m_connector;
  }

  private void setConnector(JMXConnector connector) {
    this.m_connector = connector;
  }

  public void addConnectionNotificationListener(NotificationListener arg0, NotificationFilter arg1, Object arg2) {
    getConnector().addConnectionNotificationListener(arg0, arg1, arg2);
  }

  public void close() throws IOException {
    getConnector().close();
  }

  public void connect() throws IOException {
    setConnector(m_connectManager.getJmxConnector());
    getConnector().connect(m_connectManager.getConnectionEnvironment());
  }
  
  public synchronized void connect(Map env) throws IOException {
    setConnector(m_connectManager.getJmxConnector());
    getConnector().connect(env);
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
}
