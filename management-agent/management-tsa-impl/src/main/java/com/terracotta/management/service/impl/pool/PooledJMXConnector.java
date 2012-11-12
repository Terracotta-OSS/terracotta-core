/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.pool;

import java.io.IOException;
import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

/**
 * @author Ludovic Orban
 */
public class PooledJMXConnector implements JMXConnector {

  private final JMXConnector delegate;
  private final JmxConnectorPool pool;
  private final String url;
  private volatile boolean closed = false;

  PooledJMXConnector(JMXConnector delegate, JmxConnectorPool pool, String url) {
    this.delegate = delegate;
    this.pool = pool;
    this.url = url;
  }

  @Override
  public void connect() throws IOException {
    assertNotClosed();
    delegate.connect();
  }

  @Override
  public void connect(Map<String, ?> env) throws IOException {
    assertNotClosed();
    delegate.connect(env);
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    assertNotClosed();
    return delegate.getMBeanServerConnection();
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
    assertNotClosed();
    return delegate.getMBeanServerConnection(delegationSubject);
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      pool.releaseConnector(url);
    }
  }

  @Override
  public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
    assertNotClosed();
    delegate.addConnectionNotificationListener(listener, filter, handback);
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
    assertNotClosed();
    delegate.removeConnectionNotificationListener(listener);
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
    assertNotClosed();
    delegate.removeConnectionNotificationListener(l, f, handback);
  }

  @Override
  public String getConnectionId() throws IOException {
    assertNotClosed();
    return delegate.getConnectionId();
  }

  private void assertNotClosed() {
    if (closed) {
      throw new RuntimeException("Connector is closed");
    }
  }
}
