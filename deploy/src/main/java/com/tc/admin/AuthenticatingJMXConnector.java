package com.tc.admin;

import com.tc.admin.model.IClusterModel;

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

  private IClusterModel clusterModel;
  private JMXConnector  connector;

  public AuthenticatingJMXConnector(IClusterModel clusterModel) {
    this.clusterModel = clusterModel;
  }

  private synchronized JMXConnector getConnector() {
    return connector;
  }

  private synchronized void setConnector(JMXConnector connector) {
    this.connector = connector;
  }

  public void addConnectionNotificationListener(NotificationListener arg0, NotificationFilter arg1, Object arg2) {
    getConnector().addConnectionNotificationListener(arg0, arg1, arg2);
  }

  public void close() throws IOException {
    getConnector().close();
  }

  public void connect() throws IOException {
    connect(clusterModel.getConnectionEnvironment());
  }

  public synchronized void connect(Map env) throws IOException {
    setConnector(clusterModel.getJMXConnector());
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
