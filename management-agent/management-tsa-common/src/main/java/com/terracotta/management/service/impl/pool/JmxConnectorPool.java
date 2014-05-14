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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * @author Ludovic Orban
 */
public class JmxConnectorPool {

  private final ConcurrentMap<String, PooledJMXConnector> connectorsMap = new ConcurrentHashMap<String, PooledJMXConnector>();
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
    while (true) {
      PooledJMXConnector pooledJmxConnector = connectorsMap.get(url);

      if (pooledJmxConnector == null) {
        synchronized (lock) {
          pooledJmxConnector = connectorsMap.get(url);
          if (pooledJmxConnector == null) {
            try {
              JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url), env);
              pooledJmxConnector = new PooledJMXConnector(connector);
              connectorsMap.put(url, pooledJmxConnector);
            } catch (IOException ioe) {
              // cannot create connector, server is down
              throw ioe;
            }
          }
        }
      }

      try {
        JMXConnector jmxConnector = pooledJmxConnector.getJmxConnector();
        jmxConnector.getMBeanServerConnection().getMBeanCount();
        return new JMXConnectorHolder(jmxConnector, this, url);
      } catch (IOException ioe) {
        // dead connection, killing it and retrying...
        if (pooledJmxConnector.markBroken()) {
          connectorsMap.remove(url);
        }
      }

      // the JMXConnector is broken, wait a bit before trying to recreate it
      Thread.sleep(100);
    }
  }

  protected Map<String, Object> createJmxConnectorEnv(String host, int port) {
    return null;
  }

  void releaseConnector(String url) {
    // nop
  }

  public void shutdown() {
    Collection<PooledJMXConnector> values = connectorsMap.values();
    for (PooledJMXConnector pooledJmxConnector : values) {
      try {
        pooledJmxConnector.getJmxConnector().close();
      } catch (IOException ioe) {
        // ignore
      }
    }
    connectorsMap.clear();
  }

  private final static class PooledJMXConnector {

    private final JMXConnector jmxConnector;
    private final AtomicBoolean broken = new AtomicBoolean(false);

    private PooledJMXConnector(JMXConnector jmxConnector) {
      this.jmxConnector = jmxConnector;
    }

    public JMXConnector getJmxConnector() throws IOException {
      if (broken.get()) {
        throw new IOException("broken JMX connector");
      }
      return jmxConnector;
    }

    private boolean markBroken() {
      return broken.compareAndSet(false, true);
    }
  }

}
