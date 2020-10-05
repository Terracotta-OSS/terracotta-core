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

import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.util.Assert;
import com.tc.util.State;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * When the peer node doesn't reply for the PING probes, an extra check(on demand) is made to make sure if it is really
 * dead. Today's heuristic to detect the Long GC is to connect to some of the peer listener ports. If it succeeds, we
 * will cycle again the probe sends.
 * 
 * @author Manoj
 */
public class HealthCheckerSocketConnectImpl implements HealthCheckerSocketConnect {

  private final TCSocketAddress      peerNodeAddr;
  private final TCConnection         conn;
  private final Logger logger;
  private final int                  timeoutInterval;
  private final String               remoteNodeDesc;
  private final CopyOnWriteArrayList<HealthCheckerSocketConnectEventListener> listeners  = new CopyOnWriteArrayList<HealthCheckerSocketConnectEventListener>();
  private State                      currentState;
  private short                      socketConnectNoReplyWaitCount = 0;

  // Socket Connect probes
  private static final State         SOCKETCONNECT_IDLE            = new State("SOCKETCONNECT_IDLE");
  private static final State         SOCKETCONNECT_IN_PROGRESS     = new State("SOCKETCONNECT_IN_PROGRESS");
  private static final State         SOCKETCONNECT_FAIL            = new State("SOCKETCONNECT_FAIL");

  public HealthCheckerSocketConnectImpl(TCSocketAddress peerNode, TCConnection conn, String remoteNodeDesc,
                                        Logger logger, int timeoutInterval) {
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

  @Override
  public synchronized SocketConnectStartStatus start() {
    Assert.eval(!currentState.equals(SOCKETCONNECT_IN_PROGRESS));
    socketConnectNoReplyWaitCount = 0;
    try {
      changeState(SOCKETCONNECT_IN_PROGRESS);
      conn.addListener(this);
      conn.asynchConnect(peerNodeAddr);
    } catch (IOException e) {
      conn.removeListener(this);
      changeState(SOCKETCONNECT_FAIL);
      logger.info("Socket Connect to " + remoteNodeDesc + " failed: " + e);
      return SocketConnectStartStatus.FAILED;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Socket Connect triggered for " + remoteNodeDesc);
    }
    return SocketConnectStartStatus.STARTED;
  }

  @Override
  public void stop() {
    if (conn != null) {
      conn.removeListener(this);
      conn.asynchClose();
    }
  }

  @Override
  public void addSocketConnectEventListener(HealthCheckerSocketConnectEventListener socketConnectListener) {
    if (!listeners.addIfAbsent(socketConnectListener)) { throw new AssertionError(
                                                                                  "Attempt to add same socket connect event listener moere than once: "
                                                                                      + socketConnectListener); }
  }

  @Override
  public void removeSocketConnectEventListener(HealthCheckerSocketConnectEventListener socketConnectListener) {
    if (!listeners.remove(socketConnectListener)) { throw new AssertionError(
                                                                             "Attempt to remove non registered socket connect event listener"); }
  }

  /*
   * Returns true if connection is still in progress.
   */
  @Override
  public synchronized boolean probeConnectStatus() {
    if (currentState == SOCKETCONNECT_FAIL) {
      // prev async connect failed
      logger.info("Socket Connect to " + remoteNodeDesc + " listener port failed. Probably not reachable.");
      return false;
    }

    socketConnectNoReplyWaitCount++;

    if (socketConnectNoReplyWaitCount > this.timeoutInterval) {
      logger.info("Socket Connect to " + remoteNodeDesc + " taking long time. probably not reachable.");
      stop();
      changeState(SOCKETCONNECT_FAIL);
      return false;
    }

    if (logger.isDebugEnabled()) logger.debug("Socket Connect to " + remoteNodeDesc + " listener port in progress.");
    return true;
  }

  @Override
  public synchronized void closeEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void connectEvent(TCConnectionEvent event) {

    synchronized (this) {
      stop();
      changeState(SOCKETCONNECT_IDLE);
    }

    for (HealthCheckerSocketConnectEventListener listener : listeners) {
      listener.notifySocketConnectSuccess(event);
    }
  }

  @Override
  public void endOfFileEvent(TCConnectionEvent event) {

    synchronized (this) {
      stop();
      changeState(SOCKETCONNECT_FAIL);
    }

    for (HealthCheckerSocketConnectEventListener listener : listeners) {
      listener.notifySocketConnectFail(event);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Socket Connect EOF event:" + event.toString() + " on " + remoteNodeDesc);
    }
  }

  @Override
  public void errorEvent(TCConnectionErrorEvent errorEvent) {

    synchronized (this) {
      stop();
      changeState(SOCKETCONNECT_FAIL);
    }

    for (HealthCheckerSocketConnectEventListener listener : listeners) {
      listener.notifySocketConnectFail(errorEvent);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Socket Connect Error Event:" + errorEvent.toString() + " on " + remoteNodeDesc);
    }
  }

}
