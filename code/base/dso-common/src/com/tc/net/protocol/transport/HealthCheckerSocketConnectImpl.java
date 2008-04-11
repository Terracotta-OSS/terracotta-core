/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.util.Assert;
import com.tc.util.State;

import java.io.IOException;

/**
 * When the peer node doesn't reply for the PING probes, an extra check(on demand) is made to make sure if it is really
 * dead. Today's heuristic to detect the Long GC is to connect to some of the peer listener ports. If it succeeds, we
 * will cycle again the probe sends.
 *
 * @author Manoj
 */
public class HealthCheckerSocketConnectImpl implements HealthCheckerSocketConnect {

  private final TCSocketAddress peerNodeAddr;
  private final TCConnection    conn;
  private final TCLogger        logger;
  private final int             timeoutInterval;
  private final String          remoteNodeDesc;
  private State                 currentState;
  private short                 socketConnectNoReplyWaitCount = 0;

  // Socket Connect probes
  private static final State    SOCKETCONNECT_IDLE            = new State("SOCKETCONNECT_IDLE");
  private static final State    SOCKETCONNECT_IN_PROGRESS     = new State("SOCKETCONNECT_IN_PROGRESS");
  private static final State    SOCKETCONNECT_FAIL            = new State("SOCKETCONNECT_FAIL");

  public HealthCheckerSocketConnectImpl(TCSocketAddress peerNode, TCConnection conn, String remoteNodeDesc,
                                        TCLogger logger, int timeoutInterval) {
    this.conn = conn;
    this.peerNodeAddr = peerNode;
    this.remoteNodeDesc = remoteNodeDesc;
    this.logger = logger;
    this.timeoutInterval = timeoutInterval;
    this.currentState = SOCKETCONNECT_IDLE;
  }

  /* the callers of this method are synchronized */
  private void changeState(State newState) {
    if (logger.isDebugEnabled()) {
      if (currentState != newState) logger.debug("Socket Connect Context state change for " + remoteNodeDesc + " : "
                                                 + currentState.toString() + " ===> " + newState.toString());
    }
    currentState = newState;
  }

  public synchronized boolean start() {
    Assert.eval(!currentState.equals(SOCKETCONNECT_IN_PROGRESS));
    socketConnectNoReplyWaitCount = 0;
    try {
      conn.addListener(this);
      conn.asynchConnect(peerNodeAddr);
    } catch (IOException e) {
      conn.removeListener(this);
      changeState(SOCKETCONNECT_FAIL);
      return false;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Detecting Long GC for " + remoteNodeDesc + ". Socket Connect triggered");
    }
    changeState(SOCKETCONNECT_IN_PROGRESS);
    return true;
  }

  /*
   * Returns true if connection is still in progress.
   */
  public synchronized boolean probeConnectStatus() {
    if (currentState == SOCKETCONNECT_FAIL) {
      // prev async connect failed
      logger.info("Socket Connect to " + remoteNodeDesc + " listener port failed. Probably DEAD");
      return false;
    }

    socketConnectNoReplyWaitCount++;

    if (socketConnectNoReplyWaitCount > this.timeoutInterval) {
      logger.info("Socket Connect to " + remoteNodeDesc + " taking long time. probably DEAD");
      conn.removeListener(this);
      changeState(SOCKETCONNECT_FAIL);
      return false;
    }

    if (logger.isDebugEnabled()) logger.debug("Socket Connect to " + remoteNodeDesc + " listener port in progress.");
    return true;
  }

  private void reset() {
    socketConnectNoReplyWaitCount = 0;
    changeState(SOCKETCONNECT_IDLE);
  }

  public synchronized void closeEvent(TCConnectionEvent event) {
    changeState(SOCKETCONNECT_IDLE);
  }

  public synchronized void connectEvent(TCConnectionEvent event) {
    // Async connect goes thru
    conn.asynchClose();
    reset();
    changeState(SOCKETCONNECT_IDLE);
  }

  public synchronized void endOfFileEvent(TCConnectionEvent event) {
    if (logger.isDebugEnabled()) {
      logger.debug("Socket Connect EOF event:" + event.toString() + " on " + remoteNodeDesc);
    }
    changeState(SOCKETCONNECT_FAIL);
  }

  public synchronized void errorEvent(TCConnectionErrorEvent errorEvent) {
    if (logger.isDebugEnabled()) {
      logger.debug("Socket Connect Error Event:" + errorEvent.toString() + " on " + remoteNodeDesc);
    }
    changeState(SOCKETCONNECT_FAIL);
  }

}
