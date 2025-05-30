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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.net.protocol.transport.HealthCheckerSocketConnect.SocketConnectStartStatus;
import com.tc.util.Assert;
import com.tc.util.State;
import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A HealthChecker Context takes care of sending and receiving probe signals, book-keeping, sending additional probes
 * and all the logic to monitor peers health. One Context per Transport is assigned as soon as a TC Connection is
 * Established.
 * 
 * @author Manoj
 */

class ConnectionHealthCheckerContextImpl implements ConnectionHealthCheckerContext,
    HealthCheckerSocketConnectEventListener {

  // Probe State-Flow
  private static final State                     INIT                       = new State("INIT");
  private static final State                     START                      = new State("START");
  private static final State                     ALIVE                      = new State("ALIVE");
  private static final State                     AWAIT_PINGREPLY            = new State("AWAIT_PINGREPLY");
  private static final State                     SOCKET_CONNECT             = new State("SOCKET_CONNECT");
  private static final State                     DEAD                       = new State("DEAD");

  // constant
  public static final int                        CONFIG_UPGRADE_FACTOR      = 3;

  // Basic Ping probes
  private State                                  currentState;
  private final Logger logger;
  private final MessageTransportBase             transport;
  private final HealthCheckerProbeMessageFactory messageFactory;
  private final TCConnectionManager              connectionManager;
  private final int                              maxProbeCountWithoutReply;
  private final AtomicLong                       probeReplyNotRecievedCount = new AtomicLong(0);

  // Context info
  private final HealthCheckerConfig              config;
  private final String                           remoteNodeDesc;

  // Socket Connect probes
  private int                                    intervalTimeElapsedCount   = 0;
  private int                                    idleTimeElapsedCount       = 0;
  private int                                    socketConnectSuccessCount  = 0;
  private int                                    configFactor;
  private TCConnection                           presentConnection          = null;
  private HealthCheckerSocketConnect             sockectConnect             = new NullHealthCheckerSocketConnectImpl();

  // stats
  private final AtomicLong                       pingProbeSentCount         = new AtomicLong(0);

  private final long                             timeDiffThreshold;

  public ConnectionHealthCheckerContextImpl(MessageTransportBase mtb, HealthCheckerConfig config,
                                            TCConnectionManager connMgr) {
    this.transport = mtb;
    this.messageFactory = new TransportMessageFactoryImpl();
    this.maxProbeCountWithoutReply = config.getPingProbes();
    this.config = config;
    this.connectionManager = connMgr;
    this.timeDiffThreshold = config.getTimeDiffThreshold();
    this.logger = LoggerFactory.getLogger(ConnectionHealthCheckerImpl.class.getName() + ". "
                                          + config.getHealthCheckerName());
    this.remoteNodeDesc = mtb.getRemoteAddress().toString();
    logger.info("Health monitoring agent started for " + remoteNodeDesc);
    currentState = INIT;
    configFactor = 1;
    changeState(START);
  }
  
  @Override
  public synchronized void close() {
    changeState(DEAD);
    sockectConnect.stop();
  }

  /* all callers of this method are already synchronized */
  private void changeState(State newState) {
    if (logger.isDebugEnabled() && currentState != newState) {
      logger.debug("Context state change for " + remoteNodeDesc + " : " + currentState.toString() + " ===> "
                   + newState.toString());
    }
    currentState = newState;
  }

  private boolean canPingProbe() {
    if (logger.isDebugEnabled()) {
      if (this.probeReplyNotRecievedCount.get() > 0) logger.debug("PING_REPLY not received from " + remoteNodeDesc
                                                                  + " for " + this.probeReplyNotRecievedCount
                                                                  + " times (max allowed:"
                                                                  + getMaxProbeCountWithoutReply() + ").");
    }

    return ((this.probeReplyNotRecievedCount.get() < getMaxProbeCountWithoutReply()));
  }

  private int getMaxProbeCountWithoutReply() {
    return this.maxProbeCountWithoutReply * this.configFactor;
  }

  private SocketConnectStartStatus initSocketConnectProbe() {
    // trigger the socket connect
    try {
      presentConnection = getNewConnection(connectionManager);
      sockectConnect = getHealthCheckerSocketConnector(presentConnection, transport, logger, config);
      sockectConnect.addSocketConnectEventListener(this);
      SocketConnectStartStatus status = sockectConnect.start();
      if ((status == SocketConnectStartStatus.FAILED) || (status == SocketConnectStartStatus.NOT_STARTED)) {
        clearPresentConnection();
      }
      return status;
    } catch (IOException ie) {
      logger.warn("failed to create a new connection for health check", ie);
      return SocketConnectStartStatus.FAILED;
    }
  }

  protected TCConnection getNewConnection(TCConnectionManager connManager) throws IOException {
    TCConnection connection = connManager.createConnection(new NullProtocolAdaptor());
    if (connection == null) {
      throw new IOException("failed to create a new connection");
    }
    return connection;
  }

  protected HealthCheckerSocketConnect getHealthCheckerSocketConnector(TCConnection connection,
                                                                       MessageTransportBase transportBase,
                                                                       Logger loger, HealthCheckerConfig cnfg) {
    InetSocketAddress sa = transportBase.getRemoteAddress();
    return new HealthCheckerSocketConnectImpl(sa, connection, remoteNodeDesc,
                                              loger, cnfg.getSocketConnectTimeout());
  }

  private void clearPresentConnection() {
    sockectConnect.removeSocketConnectEventListener(this);
    presentConnection = null;
  }

  @Override
  public synchronized void refresh() {
    initProbeCycle();
    initIntervalTimeElapsedCount();
    initIdleTimeElapsedCount();
    initSocketConnectCycle();
  }

  private void updateConfigFactor(int newFactor) {
    Assert.eval(newFactor >= 1);
    int currentConfigFactor = configFactor;
    configFactor = newFactor;
    initIntervalTimeElapsedCount();
    initIdleTimeElapsedCount();
    logger.info("Config Factor updated from  " + currentConfigFactor + " to " + configFactor);
  }

  private boolean isIntervalTimeElapsed() {
    intervalTimeElapsedCount++;
    if (intervalTimeElapsedCount >= configFactor) {
      initIntervalTimeElapsedCount();
      return true;
    } else {
      return false;
    }
  }

  private boolean isIdleTimeElapsed() {
    idleTimeElapsedCount++;
    if (idleTimeElapsedCount >= configFactor) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public synchronized void checkTime() {
    if (currentState.equals(START) || currentState.equals(ALIVE)) {
      try {
        sendProbeMessage(this.messageFactory.createTimeCheck(transport.getConnectionID(), transport.getConnection()));
      } catch (IOException ioe) {
        logger.warn("probe problem", ioe);
      }
    }
  }

  @Override
  public synchronized boolean probeIfAlive() {

    if (!isIntervalTimeElapsed()) { return true; }
    if (!isIdleTimeElapsed()) { return true; }

    if (currentState.equals(DEAD)) {
      // connection events might have moved us to DEAD state.
      // all return are done at the bottom
    } else if (currentState.equals(SOCKET_CONNECT)) {

      /* Socket Connect is in progress; wait for one more interval or move to next state */
      if (!sockectConnect.probeConnectStatus()) {
        changeState(DEAD);
      }

    } else if (currentState.equals(START) || currentState.equals(ALIVE) || currentState.equals(AWAIT_PINGREPLY)) {

      /* Send Probe again; if not possible move to next state */
      if (canPingProbe()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Sending PING Probe to IDLE " + remoteNodeDesc);
        }
        try {
          sendProbeMessage(this.messageFactory.createPing(transport.getConnectionID(), transport.getConnection()));
        } catch (IOException ioe) {
          logger.warn("probe problem", ioe);
          return false;
        }
        pingProbeSentCount.incrementAndGet();
        probeReplyNotRecievedCount.incrementAndGet();
        changeState(AWAIT_PINGREPLY);
      } else if (config.isSocketConnectOnPingFail()) {
        changeState(SOCKET_CONNECT);
        SocketConnectStartStatus status = initSocketConnectProbe();
        if ((status == SocketConnectStartStatus.FAILED) || (status == SocketConnectStartStatus.NOT_STARTED)) {
          changeState(DEAD);
        }
      } else {
        changeState(DEAD);
      }
    } else if (currentState.equals(INIT)) {
      // callbackport initial verification not yet done. connection events didn't arrive still. lets probe the status
      if (!sockectConnect.probeConnectStatus()) {
        callbackPortVerificationFailed();
      } else {
        // verification still in progress
      }
    }

    if (currentState.equals(DEAD)) {
      logger.info(remoteNodeDesc + " is DEAD");
      return false;
    }
    return true;
  }

  private void callbackPortVerificationFailed() {
    updateConfigFactor(CONFIG_UPGRADE_FACTOR);
    changeState(START);
    logger.debug("HealthCheckCallbackPort verification FAILED for " + remoteNodeDesc);
  }

  private void callbackPortVerificationSuccess() {
    changeState(START);
    logger.debug("HealthCheckCallbackPort verification PASSED for " + remoteNodeDesc);
  }

  @Override
  public synchronized boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (message.isPing()) {
      // Echo back but no change in this health checker state
      try {
        sendProbeMessage(this.messageFactory.createPingReply(transport.getConnectionID(), transport.getConnection()));
      } catch (IOException ioe) {
        logger.warn("probe problem", ioe);
        return true;
      }
    } else if (message.isPingReply()) {
      // The peer is alive
      if (probeReplyNotRecievedCount.get() > 0) probeReplyNotRecievedCount.decrementAndGet();

      if (probeReplyNotRecievedCount.get() <= 0) {
        changeState(ALIVE);
      }

      if (wasInLongGC()) {
        initSocketConnectCycle();
      }
    } else if (message.isTimeCheck()) {
      // log a warning if threshold exceeded
      long diff = Math.abs(System.currentTimeMillis() - message.getTime());
      if (diff > timeDiffThreshold) {
        handleTimeDesync(message, diff);
      }
    } else {
      // error message thrown at transport layers
      return false;
    }
    return true;
  }

  void handleTimeDesync(HealthCheckerProbeMessage message, long diff) {
    logger.warn(String.format("%d min time difference between %s and %s has been detected",
        TimeUnit.MILLISECONDS.toMinutes(diff), message.getSource().getLocalAddress(),
        message.getSource().getRemoteAddress()));
  }

  private void sendProbeMessage(HealthCheckerProbeMessage message) throws IOException {
    this.transport.send(message);
  }

  public long getTotalProbesSent() {
    return pingProbeSentCount.get();
  }

  private void initProbeCycle() {
    probeReplyNotRecievedCount.set(0);
  }

  private void initSocketConnectCycle() {
    socketConnectSuccessCount = 0;
  }

  private void initIntervalTimeElapsedCount() {
    intervalTimeElapsedCount = 0;
  }

  private void initIdleTimeElapsedCount() {
    idleTimeElapsedCount = 0;
  }

  private boolean wasInLongGC() {
    return (socketConnectSuccessCount > 0);
  }

  private boolean canAcceptConnectionEvent(TCConnectionEvent event) {
    if ((event.getSource() == presentConnection) && (currentState == SOCKET_CONNECT)) {
      return true;
    } else {
      // connection events after wait-period OR when not in socket connect stage -- ignore
      logger.info("Unexpected connection event: " + event + ". Current state: " + currentState);
      return false;
    }
  }

  Logger getLogger() {
    return this.logger;
  }

  @Override
  public synchronized void notifySocketConnectFail(TCConnectionEvent failureEvent) {
    if (currentState.equals(INIT)) {
      callbackPortVerificationFailed();
    } else {
      if (canAcceptConnectionEvent(failureEvent)) {
        logger.warn("Socket Connect error event:" + failureEvent.toString() + " on " + remoteNodeDesc);
        changeState(DEAD);
      }
    }
  }

  @Override
  public synchronized void notifySocketConnectSuccess(TCConnectionEvent successEvent) {
    if (currentState.equals(INIT)) {
      callbackPortVerificationSuccess();
    } else {
      if (canAcceptConnectionEvent(successEvent)) {
        // Async connect goes thru
        socketConnectSuccessCount++;
        if (socketConnectSuccessCount < config.getSocketConnectMaxCount()) {
          logger.warn(remoteNodeDesc + " might be in Long GC. Ping-probe cycles completed since last reply : "
                      + socketConnectSuccessCount);
          initProbeCycle();
          changeState(ALIVE);
        } else {
          logger.error(remoteNodeDesc + " might be in Long GC. Ping-probe cycles completed since last reply : "
                       + socketConnectSuccessCount + ". But its too long. No more retries");
          changeState(DEAD);
        }
      }
    }
  }

}
