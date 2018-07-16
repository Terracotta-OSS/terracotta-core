/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Engine which does the peer health checking work. Based on the config passed, it probes the peer once in specified
 * interval. When it doesn't get a reply from the peer, it disconnects the transport.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerImpl implements ConnectionHealthChecker {

  private final Logger logger;
  private final Thread                           monitorThread;
  private final HealthCheckerMonitorThreadEngine monitorThreadEngine;

  private final SetOnceFlag                      shutdown = new SetOnceFlag();
  private final SetOnceFlag                      started  = new SetOnceFlag();

  public ConnectionHealthCheckerImpl(HealthCheckerConfig healthCheckerConfig, TCConnectionManager connManager) {
    Assert.assertNotNull(healthCheckerConfig);
    Assert.eval(healthCheckerConfig.isHealthCheckerEnabled());
    logger = LoggerFactory.getLogger(ConnectionHealthCheckerImpl.class.getName() + ": "
                                     + healthCheckerConfig.getHealthCheckerName());
    monitorThreadEngine = getHealthMonitorThreadEngine(healthCheckerConfig, connManager, logger);
    monitorThread = new Thread(monitorThreadEngine, "HealthChecker");
    monitorThread.setDaemon(true);
  }

  protected HealthCheckerMonitorThreadEngine getHealthMonitorThreadEngine(HealthCheckerConfig config,
                                                                          TCConnectionManager connectionManager,
                                                                          Logger loger) {
    return new HealthCheckerMonitorThreadEngine(config, connectionManager, loger);
  }

  @Override
  public void start() {
    if (started.attemptSet()) {
      monitorThread.start();
      logger.info("HealthChecker Started");
    } else {
      logger.warn("HealthChecker already started");
    }
  }

  @Override
  public void stop() {
    if (shutdown.attemptSet()) {
      monitorThreadEngine.stop();
// don't bother to join the monitorThread here.  shutdown should take care of all the 
// threads in the thread group
      logger.debug("HealthChecker STOP requested");
    } else {
      logger.warn("HealthChecker STOP already requested");
    }
  }

  public boolean isRunning() {
    return started.isSet();
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    // HealthChecker Ping Thread can anyway determine this in the next probe interval thru mtb.isConnected and remove it
    // from its radar. still lets do it earlier
    if (monitorThreadEngine.removeConnection(transport)) {
      TCSocketAddress remoteAddress = transport.getRemoteAddress();
      if (remoteAddress != null) {
        logger.info("Connection to [" + remoteAddress.getCanonicalStringForm()
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
    monitorThreadEngine.addConnection(transport);
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    // HealthChecker Ping Thread can anyway determine thru ping probe cycle and remove it
    // from its radar. still lets do it earlier
    if (monitorThreadEngine.removeConnection(transport)) {
      TCSocketAddress remoteAddress = transport.getRemoteAddress();
      if (remoteAddress != null) {
        logger.info("Connection to [" + remoteAddress.getCanonicalStringForm()
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
        new ConcurrentHashMap<ConnectionID, MessageTransportBase>();
    private final long                pingIdleTime;
    private final long                pingInterval;
    private final int                 pingProbes;
    private final long                checkTimeInterval;
    private final SetOnceFlag         stop          = new SetOnceFlag();
    private final HealthCheckerConfig config;
    private final Logger logger;
    private final TCConnectionManager connectionManager;
    private final AtomicLong          lastCheckTime = new AtomicLong(System.currentTimeMillis());

    public HealthCheckerMonitorThreadEngine(HealthCheckerConfig healthCheckerConfig,
                                            TCConnectionManager connectionManager, Logger logger) {
      this.pingIdleTime = healthCheckerConfig.getPingIdleTimeMillis();
      this.pingInterval = healthCheckerConfig.getPingIntervalMillis();
      this.pingProbes = healthCheckerConfig.getPingProbes();
      this.checkTimeInterval = healthCheckerConfig.getCheckTimeInterval();
      this.connectionManager = connectionManager;
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

    public void stop() {
      stop.attemptSet();
      synchronized(stop) {
        stop.notifyAll();
      }
    }

    @Override
    public void run() {
      while (true) {

        if (stop.isSet()) {
          logger.info("HealthChecker SHUTDOWN");
          return;
        }

        // same interval for all connections
        final boolean canCheckTime = canCheckTime();

        Iterator<MessageTransportBase> connectionIterator = connectionMap.values().iterator();
        while (connectionIterator.hasNext()) {
          MessageTransportBase mtb = connectionIterator.next();

          TCConnection conn = mtb.getConnection();
          if (conn == null || !mtb.isConnected()) {
            logger.info("[" + (conn == null ? null : conn.getRemoteAddress().getCanonicalStringForm())
                        + "] is not connected. Health Monitoring for this node is now disabled.");
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

        // update last check time once for all connections
        if (canCheckTime) {
          this.lastCheckTime.set(System.currentTimeMillis());
        }

        synchronized (stop) {
          try {
            stop.wait(this.pingInterval);
          } catch (InterruptedException ie) {
//  just drop the interrupt, probably stopping.  If from the outside, re-enter the loop
          }
        }
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
