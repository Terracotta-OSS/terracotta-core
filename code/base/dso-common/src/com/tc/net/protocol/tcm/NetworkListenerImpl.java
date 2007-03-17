/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

/**
 * A handle to a specific server port listener
 * 
 * @author teck
 */
class NetworkListenerImpl implements NetworkListener {
  private final ChannelManagerImpl        channelManager;
  private final TCMessageRouter           tcmRouter;
  private final CommunicationsManagerImpl commsMgr;
  private final TCSocketAddress           addr;
  private TCListener                      lsnr;
  private boolean                         started;
  private final TCMessageFactory          msgFactory;
  private final boolean                   reuseAddr;
  private final ConnectionIDFactory       connectionIdFactory;
  private final Sink                      httpSink;

  // this cstr is intentionally not public, only the Comms Manager should be
  // creating them
  NetworkListenerImpl(TCSocketAddress addr, CommunicationsManagerImpl commsMgr, ChannelManagerImpl channelManager,
                      TCMessageFactory msgFactory, TCMessageRouter router, boolean reuseAddr,
                      ConnectionIDFactory connectionIdFactory, Sink httpSink) {
    this.commsMgr = commsMgr;
    this.channelManager = channelManager;
    this.addr = addr;
    this.connectionIdFactory = connectionIdFactory;
    this.httpSink = httpSink;
    this.started = false;
    this.msgFactory = msgFactory;
    this.reuseAddr = reuseAddr;
    this.tcmRouter = router;
  }

  /**
   * Start this listener listening on the network. You probably don't want to start a listener until you have properly
   * setup your protocol routes, since you might miss messages between the time the listener is <code>start()</code>
   * 'ed and the time you add your routes.
   * 
   * @throws IOException if an IO error occurs (this will most likely be a problem binding to the specified
   *         port/address)
   */
  public synchronized void start(Set initialConnectionIDs) throws IOException {
    this.lsnr = commsMgr.createCommsListener(this.addr, this.channelManager, this.reuseAddr, initialConnectionIDs,
                                             this.connectionIdFactory, this.httpSink);
    this.started = true;
    commsMgr.registerListener(this);
  }

  public synchronized void stop(long timeout) throws TCTimeoutException {
    if (!started) { return; }

    try {
      if (lsnr != null) {
        lsnr.stop(timeout);
      }
    } finally {
      started = false;
      commsMgr.unregisterListener(this);
    }
  }

  public void routeMessageType(TCMessageType messageType, TCMessageSink sink) {
    tcmRouter.routeMessageType(messageType, sink);
  }

  /**
   * Routes a TCMessage to a sink. The hydrate sink will do the hydrate() work
   */
  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
    routeMessageType(messageType, new TCMessageSinkToSedaSink(destSink, hydrateSink));
  }

  public ChannelManager getChannelManager() {
    return channelManager;
  }

  public void addClassMapping(TCMessageType type, Class msgClass) {
    this.msgFactory.addClassMapping(type, msgClass);
  }

  public synchronized InetAddress getBindAddress() {
    if (!started) { throw new IllegalArgumentException("Listener not running"); }
    return lsnr.getBindAddress();
  }

  public synchronized int getBindPort() {
    if (!started) { throw new IllegalArgumentException("Listener not running"); }
    return lsnr.getBindPort();
  }

  public String toString() {
    try {
      return getBindAddress().getHostAddress() + ":" + getBindPort();
    } catch (Exception e) {
      return "Exception in toString(): " + e.getMessage();
    }
  }
}