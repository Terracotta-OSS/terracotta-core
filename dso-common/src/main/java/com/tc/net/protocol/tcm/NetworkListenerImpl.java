/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
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
  private final boolean                   reuseAddr;
  private final ConnectionIDFactory       connectionIdFactory;
  private final Sink                      httpSink;
  private final WireProtocolMessageSink   wireProtoMsgSnk;

  // this constructor is intentionally not public, only the Comms Manager should be creating them
  NetworkListenerImpl(final TCSocketAddress addr, final CommunicationsManagerImpl commsMgr,
                      final ChannelManagerImpl channelManager, final TCMessageFactory msgFactory,
                      final TCMessageRouter router, final boolean reuseAddr,
                      final ConnectionIDFactory connectionIdFactory, final Sink httpSink,
                      final WireProtocolMessageSink wireProtoMsgSnk) {
    this.commsMgr = commsMgr;
    this.channelManager = channelManager;
    this.addr = addr;
    this.connectionIdFactory = connectionIdFactory;
    this.httpSink = httpSink;
    this.wireProtoMsgSnk = wireProtoMsgSnk;
    this.started = false;
    this.reuseAddr = reuseAddr;
    this.tcmRouter = router;
  }

  /**
   * Start this listener listening on the network. You probably don't want to start a listener until you have properly
   * setup your protocol routes, since you might miss messages between the time the listener is <code>start()</code> 'ed
   * and the time you add your routes.
   * 
   * @throws IOException if an IO error occurs (this will most likely be a problem binding to the specified
   *         port/address)
   */
  public synchronized void start(final Set initialConnectionIDs) throws IOException {
    this.lsnr = this.commsMgr.createCommsListener(this.addr, this.channelManager, this.reuseAddr, initialConnectionIDs,
                                                  this.connectionIdFactory, this.httpSink, this.wireProtoMsgSnk);
    this.started = true;
    this.commsMgr.registerListener(this);
  }

  public synchronized void stop(final long timeout) throws TCTimeoutException {
    if (!this.started) { return; }

    try {
      if (this.lsnr != null) {
        this.lsnr.stop(timeout);
      }
    } finally {
      this.started = false;
      this.commsMgr.unregisterListener(this);
    }
  }

  public void routeMessageType(final TCMessageType messageType, final TCMessageSink sink) {
    this.tcmRouter.routeMessageType(messageType, sink);
  }

  public ChannelManager getChannelManager() {
    return this.channelManager;
  }

  public synchronized InetAddress getBindAddress() {
    if (!this.started) { throw new IllegalStateException("Listener not running"); }
    return this.lsnr.getBindAddress();
  }

  public synchronized int getBindPort() {
    if (!this.started) { throw new IllegalStateException("Listener not running"); }
    return this.lsnr.getBindPort();
  }

  @Override
  public String toString() {
    try {
      return getBindAddress().getHostAddress() + ":" + getBindPort();
    } catch (final Exception e) {
      return "Exception in toString(): " + e.getMessage();
    }
  }
}
