/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Iterator;

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

    monitorThreadEngine = new HealthCheckerMonitorThreadEngine(healthCheckerConfig, connManager, logger);
    monitorThread = new Thread(monitorThreadEngine, "HealthChecker");
    monitorThread.setDaemon(true);
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
    TCSocketAddress remoteAddress = transport.getRemoteAddress();
    if (remoteAddress != null) {
      logger.info("Connection to [" + remoteAddress.getCanonicalStringForm()
                  + "] CLOSED. Health Monitoring for this node is now disabled.");
    } else {
      logger.info("Connection CLOSED. Health Monitor for this node is disabled.");
    }
    monitorThreadEngine.removeConnection(transport);
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    //
  }

  public void notifyTransportConnected(MessageTransport transport) {
    monitorThreadEngine.addConnection(transport);
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    //
  }

  private static class HealthCheckerMonitorThreadEngine implements Runnable {
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
      ConnectionHealthCheckerContext context = new ConnectionHealthCheckerContextImpl(mtb, config, connectionManager);
      mtb.setHealthCheckerContext(context);
      connectionMap.put(transport.getConnectionId(), transport);
    }

    public void removeConnection(MessageTransport transport) {
      connectionMap.remove(transport.getConnectionId());
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

          if (!mtb.isConnected()) {
            logger.info("[" + mtb.getConnection().getRemoteAddress().getCanonicalStringForm()
                        + "] is not connected. Health Monitoring for this node is now disabled.");
            connectionIterator.remove();
            continue;
          }

          ConnectionHealthCheckerContext connContext = mtb.getHealthCheckerContext();
          if ((mtb.getConnection().getIdleReceiveTime() >= this.pingIdleTime)) {

            if (!connContext.probeIfAlive()) {
              // Connection is dead. Disconnect the transport.
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
    public int getTotalConnectionsUnderMonitor() {
      return connectionMap.size();
    }

    public long getTotalProbesSentOnAllConnections() {
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
