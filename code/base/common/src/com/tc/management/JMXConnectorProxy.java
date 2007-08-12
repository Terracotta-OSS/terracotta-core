/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.ConnectException;
import java.text.MessageFormat;
import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

public class JMXConnectorProxy implements JMXConnector {
  private String             m_host;
  private int                m_port;
  private Map                m_env;
  private JMXServiceURL      m_serviceURL;
  private JMXConnector       m_connector;
  private JMXConnector       m_connectorProxy;

  public static final String JMXMP_URI_PATTERN  = "service:jmx:jmxmp://{0}:{1}";
  public static final String JMXRMI_URI_PATTERN = "service:jmx:rmi:///jndi/rmi://{0}:{1}/jmxrmi";

  public JMXConnectorProxy(final String host, final int port, final Map env) {
    m_host = host;
    m_port = port;
    m_env = env;
    m_connectorProxy = getConnectorProxy();
  }

  public JMXConnectorProxy(final String host, final int port) {
    this(host, port, null);
  }

  private JMXConnector getConnectorProxy() {
    JMXConnector connector = (JMXConnector) Proxy.newProxyInstance(JMXConnector.class.getClassLoader(),
        new Class[] { JMXConnector.class }, new ConnectorInvocationHandler());
    return connector;
  }

  static boolean isConnectException(IOException ioe) {
    Throwable t = ioe;

    while (t != null) {
      if (t instanceof ConnectException) { return true; }
      t = t.getCause();
    }

    return false;
  }

  private void determineConnector() throws Exception {
    JMXServiceURL url = new JMXServiceURL(getSecureJMXConnectorURL(m_host, m_port));

    try {
      m_connector = JMXConnectorFactory.connect(url, m_env);
      m_serviceURL = url;
    } catch (IOException ioe) {
      if (isConnectException(ioe)) { throw ioe; }
      url = new JMXServiceURL(getJMXConnectorURL(m_host, m_port));
      m_connector = JMXConnectorFactory.connect(url, m_env);
      m_serviceURL = url;
    }
  }

  private void ensureConnector() throws Exception {
    if (m_connector == null) {
      determineConnector();
    }
  }

  class ConnectorInvocationHandler implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      ensureConnector();

      try {
        Class c = m_connector.getClass();
        Method m = c.getMethod(method.getName(), method.getParameterTypes());
        return m.invoke(m_connector, args);
      } catch (InvocationTargetException ite) {
        Throwable cause = ite.getCause();
        if (cause != null) throw cause;
        else throw ite;
      }
    }
  }

  public String getHost() {
    return m_host;
  }

  public int getPort() {
    return m_port;
  }

  public JMXServiceURL getServiceURL() {
    return m_serviceURL;
  }

  public static String getJMXConnectorURL(final String host, final int port) {
    return MessageFormat.format(JMXMP_URI_PATTERN, new Object[] { host, port + "" });
  }

  public static String getSecureJMXConnectorURL(final String host, final int port) {
    return MessageFormat.format(JMXRMI_URI_PATTERN, new Object[] { host, port + "" });
  }

  public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object data) {
    m_connectorProxy.addConnectionNotificationListener(listener, filter, data);
  }

  public void close() throws IOException {
    m_connectorProxy.close();
  }

  public void connect() throws IOException {
    m_connectorProxy.connect();
  }

  public void connect(Map env) throws IOException {
    m_connectorProxy.connect(env);
  }

  public String getConnectionId() throws IOException {
    return m_connectorProxy.getConnectionId();
  }

  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    return m_connectorProxy.getMBeanServerConnection();
  }

  public MBeanServerConnection getMBeanServerConnection(Subject subject) throws IOException {
    return m_connectorProxy.getMBeanServerConnection(subject);
  }

  public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
    m_connectorProxy.removeConnectionNotificationListener(listener);
  }

  public void removeConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object data)
      throws ListenerNotFoundException {
    m_connectorProxy.removeConnectionNotificationListener(listener, filter, data);
  }

  public String toString() {
    return m_host + ":" + m_port;
  }
}
