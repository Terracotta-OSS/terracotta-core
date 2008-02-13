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
  private State                 currentState;

  // Socket Connect probes
  public static final int       SOCKETCONNECT_NOREPLY_MAXWAIT_CYCLE = 2;
  private static final State    SOCKETCONNECT_IDLE                  = new State("SOCKETCONNECT_IDLE");
  private static final State    SOCKETCONNECT_IN_PROGRESS           = new State("SOCKETCONNECT_IN_PROGRESS");
  private static final State    SOCKETCONNECT_FAIL                  = new State("SOCKETCONNECT_FAIL");
  private short                 socketConnectNoReplyWaitCount       = 0;

  public HealthCheckerSocketConnectImpl(TCSocketAddress peerNode, TCConnection conn, TCLogger logger) {
    this.conn = conn;
    this.peerNodeAddr = peerNode;
    this.logger = logger;
    currentState = SOCKETCONNECT_IDLE;
  }

  /* the callers of this method are synchronized */
  private void changeState(State newState) {
    if (logger.isDebugEnabled()) {
      if (currentState != newState) logger.debug("Context state change - socket connect " + currentState.toString()
                                                 + " ===> " + newState.toString());
    }
    currentState = newState;
  }

  public synchronized boolean start() {
    Assert.eval(!currentState.equals(SOCKETCONNECT_IN_PROGRESS));
    socketConnectNoReplyWaitCount = 0;
    try {
      conn.asynchConnect(peerNodeAddr);
    } catch (IOException e) {
      if (conn != null) conn.removeListener(this);
      changeState(SOCKETCONNECT_FAIL);
      return false;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Detecting Long GC. Socket Connect triggered");
    }
    changeState(SOCKETCONNECT_IN_PROGRESS);
    return true;
  }

  /*
   * Returns true if connection is still in progress.
   */
  public synchronized boolean porobeConnectStatus() {

    if (currentState == SOCKETCONNECT_FAIL) {
      // prev async connect failed
      if (logger.isDebugEnabled()) logger.info("Socket Connect to peer listener port failed. Probably DEAD");
      return false;
    }

    socketConnectNoReplyWaitCount++;

    if (socketConnectNoReplyWaitCount > SOCKETCONNECT_NOREPLY_MAXWAIT_CYCLE) {
      if (logger.isDebugEnabled()) logger.debug("Socket Connect taking long time. probably DEAD");
      if (conn != null) conn.removeListener(this);
      changeState(SOCKETCONNECT_FAIL);
      return false;
    }

    if (logger.isDebugEnabled()) logger.debug("Socket Connect to peer listener port in progress.");
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
    if (logger.isDebugEnabled()) logger.debug("Peer might be in Long GC");
    conn.asynchClose();
    reset();
    changeState(SOCKETCONNECT_IDLE);
  }

  public synchronized void endOfFileEvent(TCConnectionEvent event) {
    if (logger.isDebugEnabled()) {
      logger.debug("Socket Connect EOF event:" + event.toString());
    }
    changeState(SOCKETCONNECT_FAIL);
  }

  public synchronized void errorEvent(TCConnectionErrorEvent errorEvent) {
    if (logger.isDebugEnabled()) {
      logger.debug("Socket Connect Error Event:" + errorEvent.toString());
    }
    changeState(SOCKETCONNECT_FAIL);
  }

}
