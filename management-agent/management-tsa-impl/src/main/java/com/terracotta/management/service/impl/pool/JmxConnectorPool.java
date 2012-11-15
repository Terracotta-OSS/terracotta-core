/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.pool;

import java.io.IOException;
import java.util.Collection;
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

  public JMXConnector getConnector(String url) throws IOException, InterruptedException, MalformedObjectNameException {
    JMXConnectorHolder jmxConnectorHolder = connectorsMap.get(url);

    if (jmxConnectorHolder == null) {
      synchronized (lock) {
        jmxConnectorHolder = connectorsMap.get(url);
        if (jmxConnectorHolder == null) {
          try {
            JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url), null);
            jmxConnectorHolder = new JMXConnectorHolder(connector);
            connectorsMap.put(url, jmxConnectorHolder);
          } catch (IOException ioe) {
            // cannot create connector, server is down
            throw ioe;
          }
        }
      }
    }

    jmxConnectorHolder.getLock().lock();
    JMXConnector jmxConnector = jmxConnectorHolder.getJmxConnector();
    try {
      jmxConnector.getMBeanServerConnection().getMBeanCount();
    } catch (IOException ioe) {
      // dead connection, killing it and retrying...
      connectorsMap.remove(url);
      return getConnector(url);
    }
    return new PooledJMXConnector(jmxConnector, this, url);
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

    private JMXConnectorHolder(JMXConnector jmxConnector) {
      this.jmxConnector = jmxConnector;
    }

    public Lock getLock() {
      return lock;
    }

    public JMXConnector getJmxConnector() {
      return jmxConnector;
    }
  }

}
