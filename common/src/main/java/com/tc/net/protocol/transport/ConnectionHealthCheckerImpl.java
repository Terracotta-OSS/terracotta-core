/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

import com.tc.lang.TCThreadGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import java.net.InetSocketAddress;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * The Engine which does the peer health checking work. Based on the config passed, it probes the peer once in specified
 * interval. When it doesn't get a reply from the peer, it disconnects the transport.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerImpl implements ConnectionHealthChecker {

  private final Logger logger;
  private static final ScheduledExecutorService monitorThread = createHealthCheckExecutor();
  private final HealthCheckerMonitorThreadEngine monitorThreadEngine;

  private Future<?>                      task;
  
  public ConnectionHealthCheckerImpl(HealthCheckerConfig healthCheckerConfig, TCConnectionManager connManager, Supplier<Boolean> reachable) {
    Assert.assertNotNull(healthCheckerConfig);
    Assert.eval(healthCheckerConfig.isHealthCheckerEnabled());
    logger = LoggerFactory.getLogger(ConnectionHealthCheckerImpl.class.getName() + ": "
                                     + healthCheckerConfig.getHealthCheckerName());
    monitorThreadEngine = getHealthMonitorThreadEngine(healthCheckerConfig, connManager, reachable, logger);
  }
  
  private static ScheduledExecutorService createHealthCheckExecutor() {
    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, (r) -> {
      ThreadGroup grp = Thread.currentThread().getThreadGroup();
      String id = "";
      while (grp instanceof TCThreadGroup) {
        id = "-" + grp.getName();
        grp = grp.getParent();
      }
      Thread t = new Thread(grp, r, "HealthCheck" + id);
      t.setDaemon(true);
      return t;
    });
    exec.setKeepAliveTime(5, TimeUnit.SECONDS);
    exec.allowCoreThreadTimeOut(true);
    return exec;
  }

  private HealthCheckerMonitorThreadEngine getHealthMonitorThreadEngine(HealthCheckerConfig config,
                                                                          TCConnectionManager connectionManager,
                                                                          Supplier<Boolean> reachable,
                                                                          Logger logger) {
    return new HealthCheckerMonitorThreadEngine(config, connectionManager, ()-> {
      if (reachable.get()) {
        return true;
      } else {
        if (!isStopped()) {
          stop();
          connectionManager.asynchCloseAllConnections();
          connectionManager.closeAllListeners();
          connectionManager.shutdown();
        }
        return false;
      }
    }, logger);
  }

  @Override
  public synchronized void start() {
    if (task == null) {
      try {
        task = monitorThread.scheduleAtFixedRate(monitorThreadEngine, 0L, monitorThreadEngine.pingInterval, TimeUnit.MILLISECONDS);
      } catch (IllegalStateException state) {
        logger.warn("HealthChecker cannot start");
        return;
      }
      logger.info("HealthChecker Started");
    } else {
      logger.warn("HealthChecker already started");
    }
  }

  @Override
  public synchronized void stop() {
    if (task != null && !task.isCancelled()) {
      task.cancel(true);
      logger.debug("HealthChecker STOP requested");
    }
  }
  
  private synchronized boolean isStopped() {
    return task == null || task.isCancelled();
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    // HealthChecker Ping Thread can anyway determine this in the next probe interval thru mtb.isConnected and remove it
    // from its radar. still lets do it earlier
    if (monitorThreadEngine.removeConnection(transport)) {
      InetSocketAddress remoteAddress = transport.getRemoteAddress();
      if (remoteAddress != null) {
        logger.info("Connection to [" + remoteAddress.toString()
                    + "] CLOSED. Health Monitoring for this node is now disabled.");
      } else {
        logger.info("Connection " + transport.getConnectionID() + " CLOSED. Health Monitor for this node is disabled.");
      }
    }
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    //
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    start();
    monitorThreadEngine.addConnection(transport);
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    // HealthChecker Ping Thread can anyway determine thru ping probe cycle and remove it
    // from its radar. still lets do it earlier
    if (monitorThreadEngine.removeConnection(transport)) {
      InetSocketAddress remoteAddress = transport.getRemoteAddress();
      if (remoteAddress != null) {
        logger.info("Connection to [" + remoteAddress.toString()
                    + "] DISCONNECTED. Health Monitoring for this node is now disabled.");
      } else {
        logger.info("Connection " + transport.getConnectionID()
                    + " DISCONNECTED. Health Monitor for this node is disabled.");
      }
    }
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    // NOP
  }

  static class HealthCheckerMonitorThreadEngine implements Runnable {
    private final ConcurrentMap<ConnectionID, MessageTransportBase> connectionMap =
        new ConcurrentHashMap<>();
    private final long                pingIdleTime;
    private final long                pingInterval;
    private final int                 pingProbes;
    private final long                checkTimeInterval;
    private final HealthCheckerConfig config;
    private final Logger logger;
    private final TCConnectionManager connectionManager;
    private final Supplier<Boolean>   reachable;
    private final AtomicLong          lastCheckTime = new AtomicLong(System.currentTimeMillis());

    public HealthCheckerMonitorThreadEngine(HealthCheckerConfig healthCheckerConfig,
                                            TCConnectionManager connectionManager, Supplier<Boolean> reachable, Logger logger) {
      this.pingIdleTime = healthCheckerConfig.getPingIdleTimeMillis();
      this.pingInterval = healthCheckerConfig.getPingIntervalMillis();
      this.pingProbes = healthCheckerConfig.getPingProbes();
      this.checkTimeInterval = healthCheckerConfig.getCheckTimeInterval();
      this.connectionManager = connectionManager;
      this.reachable = reachable;
      this.config = healthCheckerConfig;

      Assert.assertNotNull(logger);
      this.logger = logger;

      if ((pingIdleTime - pingInterval < 0) || pingIdleTime <= 0 || pingInterval <= 0 || pingProbes <= 0) {
        logger
            .info("ping_interval period should be less than ping_idletime and ping Ideltime/Interval/Probes cannot be 0 or negative.");
        logger.info("Disabling HealthChecker for this CommsMgr");
        throw new AssertionError("HealthChecker Config Error");
      }

    }

    private void addConnection(MessageTransport transport) {
      MessageTransportBase mtb = (MessageTransportBase) transport;
      mtb.setHealthCheckerContext(getHealthCheckerContext(mtb, config, connectionManager));
      connectionMap.put(transport.getConnectionID(), mtb);
    }

    private boolean removeConnection(MessageTransport transport) {
      return (connectionMap.remove(transport.getConnectionID())) != null;
    }

    protected ConnectionHealthCheckerContext getHealthCheckerContext(MessageTransportBase transport,
                                                                     HealthCheckerConfig conf,
                                                                     TCConnectionManager connManager) {
      return new ConnectionHealthCheckerContextImpl(transport, conf, connManager);
    }

    @Override
    public void run() {
      // same interval for all connections
      final boolean canCheckTime = canCheckTime();

      if (reachable.get()) {
        Iterator<MessageTransportBase> connectionIterator = connectionMap.values().iterator();
        while (connectionIterator.hasNext()) {
          MessageTransportBase mtb = connectionIterator.next();

          TCConnection conn = mtb.getConnection();
          if (conn == null || !mtb.isConnected()) {
            logger.info("[" + (conn == null ? null : conn.getRemoteAddress().toString())
                        + "] is not connected. Health Monitoring for this node is now disabled.");
            connectionIterator.remove();
            continue;
          }

          if (mtb.getReceiveLayer() == null) {
            logger.info("[" + (conn == null ? null : conn.getRemoteAddress().toString())
                        + "] is no longer referenced.  Closing the connection");
            mtb.disconnect();
            connectionIterator.remove();
            continue;
          }

          ConnectionHealthCheckerContext connContext = mtb.getHealthCheckerContext();
          if ((conn.getIdleReceiveTime() >= this.pingIdleTime)) {

            if (!connContext.probeIfAlive()) {
              // Connection is dead. Disconnect the transport.
              logger.error("Declared connection dead " + mtb.getConnectionID() + " idle time "
                           + conn.getIdleReceiveTime() + "ms");
              mtb.disconnect();
              connectionIterator.remove();
            }
          } else {
            connContext.refresh();
          }
          // is there any significant time difference between hosts ?
          if (canCheckTime) {
            connContext.checkTime();
          }
        }
      }

      // update last check time once for all connections
      if (canCheckTime) {
        this.lastCheckTime.set(System.currentTimeMillis());
      }
    }

    boolean canCheckTime() {
      return config.isCheckTimeEnabled() &&
             (System.currentTimeMillis() - this.lastCheckTime.get() >= this.checkTimeInterval);
    }

    /* For testing only */
    int getTotalConnectionsUnderMonitor() {
      return connectionMap.size();
    }

    long getTotalProbesSentOnAllConnections() {
      Iterator<MessageTransportBase> connIterator = connectionMap.values().iterator();
      long totalProbeSent = 0;
      while (connIterator.hasNext()) {
        MessageTransportBase mtb = connIterator.next();
        ConnectionHealthCheckerContextImpl connContext = (ConnectionHealthCheckerContextImpl) mtb
            .getHealthCheckerContext();
        totalProbeSent += connContext.getTotalProbesSent();
      }
      return totalProbeSent;
    }
  }

  /* For testing only */
  public int getTotalConnsUnderMonitor() {
    return monitorThreadEngine.getTotalConnectionsUnderMonitor();
  }

  public long getTotalProbesSentOnAllConns() {
    return monitorThreadEngine.getTotalProbesSentOnAllConnections();
  }
}
