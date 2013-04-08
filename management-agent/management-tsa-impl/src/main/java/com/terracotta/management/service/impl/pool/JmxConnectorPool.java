/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.pool;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * @author Ludovic Orban
 */
public class JmxConnectorPool {

  private final ConcurrentMap<String, JMXConnectorHolder> connectorsMap = new ConcurrentHashMap<String, JMXConnectorHolder>();
  private final Object lock = new Object();
  private final String urlPattern;

  public JmxConnectorPool(String urlPattern) {
    this.urlPattern = urlPattern;
  }

  public JMXConnector getConnector(String host, int port) throws IOException, InterruptedException, MalformedObjectNameException {
    String url = MessageFormat.format(urlPattern, host, "" + port);
    Map<String, Object> env = createJmxConnectorEnv(host, port);
    return getConnector(url, env);
  }

  private JMXConnector getConnector(String url, Map<String, Object> env) throws IOException, InterruptedException, MalformedObjectNameException {
    JMXConnectorHolder jmxConnectorHolder = connectorsMap.get(url);

    if (jmxConnectorHolder == null) {
      synchronized (lock) {
        jmxConnectorHolder = connectorsMap.get(url);
        if (jmxConnectorHolder == null) {
          try {
            JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url), env);
            jmxConnectorHolder = new JMXConnectorHolder(connector);
            connectorsMap.put(url, jmxConnectorHolder);
          } catch (IOException ioe) {
            // cannot create connector, server is down
            throw ioe;
          }
        }
      }
    }

    try {
      jmxConnectorHolder.getLock().lock();
      JMXConnector jmxConnector = jmxConnectorHolder.getJmxConnector();
      jmxConnector.getMBeanServerConnection().getMBeanCount();
      return new PooledJMXConnector(jmxConnector, this, url);
    } catch (IOException ioe) {
      // dead connection, killing it and retrying...
      if (!jmxConnectorHolder.isBroken()) {
        connectorsMap.remove(url);
        jmxConnectorHolder.setBroken(true);
        jmxConnectorHolder.getLock().unlock();
      }
      return getConnector(url, env);
    }
  }

  protected Map<String, Object> createJmxConnectorEnv(String host, int port) {
    return null;
  }

  void releaseConnector(String url) {
    JMXConnectorHolder jmxConnectorHolder = connectorsMap.get(url);
    if (jmxConnectorHolder != null) {
      jmxConnectorHolder.getLock().unlock();
    }
  }

  public void shutdown() {
    Collection<JMXConnectorHolder> values = connectorsMap.values();
    for (JMXConnectorHolder jmxConnectorHolder : values) {
      jmxConnectorHolder.getLock().lock();
      try {
        jmxConnectorHolder.getJmxConnector().close();
      } catch (IOException ioe) {
        // ignore
      }
      jmxConnectorHolder.getLock().unlock();
    }
    connectorsMap.clear();
  }

  private final static class JMXConnectorHolder {

    private final Lock lock = new ReentrantLock();
    private final JMXConnector jmxConnector;
    private volatile boolean broken;

    private JMXConnectorHolder(JMXConnector jmxConnector) {
      this.jmxConnector = jmxConnector;
    }

    public Lock getLock() {
      return lock;
    }

    public JMXConnector getJmxConnector() throws IOException {
      if (broken) {
        throw new IOException("broken JMX connector");
      }
      return jmxConnector;
    }

    private boolean isBroken() {
      return broken;
    }

    private void setBroken(boolean broken) {
      this.broken = broken;
    }
  }

}
