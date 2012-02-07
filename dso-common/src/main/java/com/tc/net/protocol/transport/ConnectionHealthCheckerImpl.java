/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Engine which does the peer health checking work. Based on the config passed, it probes the peer once in specified
 * interval. When it doesn't get a reply from the peer, it disconnects the transport.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerImpl implements ConnectionHealthChecker {

  private final TCLogger                         logger;
  private final Thread                           monitorThread;
  private final HealthCheckerMonitorThreadEngine monitorThreadEngine;

  private final SetOnceFlag                      shutdown = new SetOnceFlag();
  private final SetOnceFlag                      started  = new SetOnceFlag();

  public ConnectionHealthCheckerImpl(HealthCheckerConfig healthCheckerConfig, TCConnectionManager connManager) {
    Assert.assertNotNull(healthCheckerConfig);
    Assert.eval(healthCheckerConfig.isHealthCheckerEnabled());
    logger = TCLogging.getLogger(ConnectionHealthCheckerImpl.class.getName() + ": "
                                 + healthCheckerConfig.getHealthCheckerName());
    logger.setLevel(LogLevelImpl.DEBUG);
    monitorThreadEngine = getHealthMonitorThreadEngine(healthCheckerConfig, connManager, logger);
    monitorThread = new Thread(monitorThreadEngine, "HealthChecker");
    monitorThread.setDaemon(true);
  }

  protected HealthCheckerMonitorThreadEngine getHealthMonitorThreadEngine(HealthCheckerConfig config,
                                                                          TCConnectionManager connectionManager,
                                                                          TCLogger loger) {
    return new HealthCheckerMonitorThreadEngine(config, connectionManager, loger);
  }

  public void start() {
    if (started.attemptSet()) {
      monitorThread.start();
      logger.info("HealthChecker Started");
    } else {
      logger.warn("HealthChecker already started");
    }
  }

  public void stop() {
    if (shutdown.attemptSet()) {
      monitorThreadEngine.stop();
      logger.info("HealthChecker STOP requested");
    } else {
      logger.info("HealthChecker STOP already requested");
    }
  }

  public boolean isRunning() {
    return started.isSet();
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // HealthChecker Ping Thread can anyway determine this in the next probe interval thru mtb.isConnected and remove it
    // from its radar. still lets do it earlier
    if (monitorThreadEngine.removeConnection(transport)) {
      TCSocketAddress remoteAddress = transport.getRemoteAddress();
      if (remoteAddress != null) {
        logger.info("Connection to [" + remoteAddress.getCanonicalStringForm()
                    + "] CLOSED. Health Monitoring for this node is now disabled.");
      } else {
        logger.info("Connection " + transport.getConnectionId() + " CLOSED. Health Monitor for this node is disabled.");
      }
    }
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    //
  }

  public void notifyTransportConnected(MessageTransport transport) {
    monitorThreadEngine.addConnection(transport);
  }

  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    // HealthChecker Ping Thread can anyway determine thru ping probe cycle and remove it
    // from its radar. still lets do it earlier
    if (monitorThreadEngine.removeConnection(transport)) {
      TCSocketAddress remoteAddress = transport.getRemoteAddress();
      if (remoteAddress != null) {
        logger.info("Connection to [" + remoteAddress.getCanonicalStringForm()
                    + "] DISCONNECTED. Health Monitoring for this node is now disabled.");
      } else {
        logger.info("Connection " + transport.getConnectionId()
                    + " DISCONNECTED. Health Monitor for this node is disabled.");
      }
    }
  }

  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    // NOP
  }

  static class HealthCheckerMonitorThreadEngine implements Runnable {
    private final ConcurrentHashMap   connectionMap = new ConcurrentHashMap();
    private final long                pingIdleTime;
    private final long                pingInterval;
    private final int                 pingProbes;
    private final SetOnceFlag         stop          = new SetOnceFlag();
    private final HealthCheckerConfig config;
    private final TCLogger            logger;
    private final TCConnectionManager connectionManager;

    public HealthCheckerMonitorThreadEngine(HealthCheckerConfig healthCheckerConfig,
                                            TCConnectionManager connectionManager, TCLogger logger) {
      this.pingIdleTime = healthCheckerConfig.getPingIdleTimeMillis();
      this.pingInterval = healthCheckerConfig.getPingIntervalMillis();
      this.pingProbes = healthCheckerConfig.getPingProbes();
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

    public void addConnection(MessageTransport transport) {
      MessageTransportBase mtb = (MessageTransportBase) transport;
      mtb.setHealthCheckerContext(getHealthCheckerContext(mtb, config, connectionManager));
      connectionMap.put(transport.getConnectionId(), transport);
    }

    public boolean removeConnection(MessageTransport transport) {
      if ((connectionMap.remove(transport.getConnectionId())) != null) { return true; }
      return false;
    }

    protected ConnectionHealthCheckerContext getHealthCheckerContext(MessageTransportBase transport,
                                                                     HealthCheckerConfig conf,
                                                                     TCConnectionManager connManager) {
      return new ConnectionHealthCheckerContextImpl(transport, conf, connManager);
    }

    public void stop() {
      stop.attemptSet();
    }

    public void run() {
      while (true) {

        if (stop.isSet()) {
          logger.info("HealthChecker SHUTDOWN");
          return;
        }

        Iterator connectionIterator = connectionMap.values().iterator();
        while (connectionIterator.hasNext()) {
          MessageTransportBase mtb = (MessageTransportBase) connectionIterator.next();

          TCConnection conn = mtb.getConnection();
          if (conn == null || !mtb.isConnected()) {
            logger.info("[" + conn == null ? null
                : conn.getRemoteAddress().getCanonicalStringForm()
                  + "] is not connected. Health Monitoring for this node is now disabled.");
            connectionIterator.remove();
            continue;
          }

          ConnectionHealthCheckerContext connContext = mtb.getHealthCheckerContext();
          if ((conn.getIdleReceiveTime() >= this.pingIdleTime)) {

            if (!connContext.probeIfAlive()) {
              // Connection is dead. Disconnect the transport.
              logger.error("Declared connection dead " + mtb.getConnectionId() + " idle time "
                           + conn.getIdleReceiveTime() + "ms");
              mtb.disconnect();
              connectionIterator.remove();
            }

          } else {
            connContext.refresh();
          }
        }

        ThreadUtil.reallySleep(this.pingInterval);
      }
    }

    /* For testing only */
    int getTotalConnectionsUnderMonitor() {
      return connectionMap.size();
    }

    long getTotalProbesSentOnAllConnections() {
      Iterator connIterator = connectionMap.values().iterator();
      long totalProbeSent = 0;
      while (connIterator.hasNext()) {
        MessageTransportBase mtb = (MessageTransportBase) connIterator.next();
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
