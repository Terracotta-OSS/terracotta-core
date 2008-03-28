/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.util.State;

/**
 * A HealthChecker Context takes care of sending and receiving probe signals, book-keeping, sending additional probes
 * and all the logic to monitor peers health. One Context per Transport is assigned as soon as a TC Connection is
 * Established.
 * 
 * @author Manoj
 */

class ConnectionHealthCheckerContextImpl implements ConnectionHealthCheckerContext, TCConnectionEventListener {

  // Probe State-Flow
  private static final State                     START                      = new State("START");
  private static final State                     ALIVE                      = new State("ALIVE");
  private static final State                     AWAIT_PINGREPLY            = new State("AWAIT_PINGREPLY");
  private static final State                     SOCKET_CONNECT             = new State("SOCKET_CONNECT");
  private static final State                     DEAD                       = new State("DEAD");

  // Basic Ping probes
  private State                                  currentState;
  private final TCLogger                         logger;
  private final MessageTransportBase             transport;
  private final HealthCheckerProbeMessageFactory messageFactory;
  private final TCConnectionManager              connectionManager;
  private final int                              maxProbeCountWithoutReply;
  private final SynchronizedLong                 probeReplyNotRecievedCount = new SynchronizedLong(0);

  // Context info
  private final HealthCheckerConfig              config;

  // Socket Connect probes
  private int                                    socketConnectSuccessCount  = 0;
  private TCConnection                           conn;
  private HealthCheckerSocketConnect             sockectConnect;

  // stats
  private final SynchronizedLong                 pingProbeSentCount         = new SynchronizedLong(0);

  public ConnectionHealthCheckerContextImpl(MessageTransportBase mtb, HealthCheckerConfig config,
                                            TCConnectionManager connMgr) {
    currentState = START;
    this.transport = mtb;
    this.messageFactory = new TransportMessageFactoryImpl();
    this.maxProbeCountWithoutReply = config.getPingProbes();
    this.config = config;
    this.connectionManager = connMgr;
    this.logger = TCLogging.getLogger(ConnectionHealthCheckerImpl.class.getName() + ". "
                                      + config.getHealthCheckerName() + "(" + mtb.getConnectionId() + ")");
    logger.info("Health monitoring agent started");
  }

  /* all callers of this method are already synchronized */
  private void changeState(State newState) {
    if (logger.isDebugEnabled() && currentState != newState) {
      logger.debug("Context state change: " + currentState.toString() + " ===> " + newState.toString());
    }
    currentState = newState;
  }

  private boolean canPingProbe() {
    if (logger.isDebugEnabled()) {
      if (this.probeReplyNotRecievedCount.get() > 0) logger.debug("PING_REPLY not received for "
                                                                  + this.probeReplyNotRecievedCount + "(max allowed:"
                                                                  + this.maxProbeCountWithoutReply + ").");
    }

    return ((this.probeReplyNotRecievedCount.get() < this.maxProbeCountWithoutReply));
  }

  private boolean initSocketConnectProbe() {

    // trigger the socket connect
    conn = connectionManager.createConnection(new NullProtocolAdaptor());
    conn.addListener(this);
    sockectConnect = new HealthCheckerSocketConnectImpl(transport.getRemoteAddress(), conn, logger);

    if (sockectConnect.start()) {
      return true;
    } else {
      conn.removeListener(this);
      return false;
    }
  }

  public synchronized void refresh() {
    initProbeCycle();
    initSocketConnectCycle();
  }

  public synchronized boolean probeIfAlive() {

    if (currentState.equals(DEAD)) {
      // connection events might have moved us to DEAD state.
      // all return are done at the bottom
    } else if (currentState.equals(SOCKET_CONNECT)) {

      /* Socket Connect is in progress; wait for one more interval or move to next state */
      if (!sockectConnect.porobeConnectStatus()) {
        changeState(DEAD);
      }

    } else if (currentState.equals(ALIVE) || currentState.equals(AWAIT_PINGREPLY)) {

      /* Send Probe again; if not possible move to next state */
      if (canPingProbe()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Sending PING Probe to this IDLE connection");
        }
        sendProbeMessage(this.messageFactory.createPing(transport.getConnectionId(), transport.getConnection()));
        pingProbeSentCount.increment();
        probeReplyNotRecievedCount.increment();
        changeState(AWAIT_PINGREPLY);
      } else if (config.isSocketConnectOnPingFail()) {
        changeState(SOCKET_CONNECT);
        if (!initSocketConnectProbe()) {
          changeState(DEAD);
        }
      } else {
        changeState(DEAD);
      }
    }

    if (currentState.equals(DEAD)) {
      logger.info("is DEAD.");
      return false;
    }
    return true;
  }

  public synchronized boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (message.isPing()) {
      // Echo back but no change in this health checker state
      sendProbeMessage(this.messageFactory.createPingReply(transport.getConnectionId(), transport.getConnection()));
    } else if (message.isPingReply()) {
      // The peer is alive
      if (probeReplyNotRecievedCount.get() > 0) probeReplyNotRecievedCount.decrement();

      if (probeReplyNotRecievedCount.compareTo(0) <= 0) {
        changeState(ALIVE);
      }

      if (wasInLongGC()) {
        initSocketConnectCycle();
      }
    } else {
      // error message thrown at transport layers
      return false;
    }
    return true;
  }

  private void sendProbeMessage(HealthCheckerProbeMessage message) {
    this.transport.send(message);
  }

  public long getTotalProbesSent() {
    return pingProbeSentCount.get();
  }

  private void initProbeCycle() {
    probeReplyNotRecievedCount.set(0);
    changeState(ALIVE);
  }

  private void initSocketConnectCycle() {
    socketConnectSuccessCount = 0;
  }

  private boolean wasInLongGC() {
    return (socketConnectSuccessCount > 0);
  }

  public synchronized void closeEvent(TCConnectionEvent event) {
    // hmm
  }

  public synchronized void connectEvent(TCConnectionEvent event) {
    // Async connect goes thru
    socketConnectSuccessCount++;
    if (socketConnectSuccessCount < config.getMaxSocketConnectCount()) {
      logger.warn("Peer might be in Long GC.");
      initProbeCycle();
    } else {
      logger.error("Peer might be in Long GC. But its too long. No more retries");
      changeState(DEAD);
    }
  }

  public synchronized void endOfFileEvent(TCConnectionEvent event) {
    logger.warn("Socket Connect EOF event:" + event.toString());
    changeState(DEAD);
  }

  public synchronized void errorEvent(TCConnectionErrorEvent errorEvent) {
    logger.error("Socket Connect Error Event:" + errorEvent.toString());
    changeState(DEAD);
  }

}
