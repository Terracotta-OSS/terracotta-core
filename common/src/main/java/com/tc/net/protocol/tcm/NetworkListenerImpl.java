/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.transport.ConnectionID;
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
  private final ChannelManagerImpl channelManager;
  private final CommunicationsManagerImpl commsMgr;
  private final TCSocketAddress addr;
  private TCListener lsnr;
  private boolean started;
  private final boolean reuseAddr;
  private final ConnectionIDFactory connectionIdFactory;
  private final WireProtocolMessageSink wireProtoMsgSnk;

  // this constructor is intentionally not public, only the Comms Manager should be creating them
  NetworkListenerImpl(TCSocketAddress addr, CommunicationsManagerImpl commsMgr, ChannelManagerImpl channelManager,
                      TCMessageFactory msgFactory, boolean reuseAddr, ConnectionIDFactory connectionIdFactory,
                      WireProtocolMessageSink wireProtoMsgSnk) {
    this.commsMgr = commsMgr;
    this.channelManager = channelManager;
    this.addr = addr;
    this.connectionIdFactory = connectionIdFactory;
    this.wireProtoMsgSnk = wireProtoMsgSnk;
    this.started = false;
    this.reuseAddr = reuseAddr;
  }

  /**
   * Start this listener listening on the network. You probably don't want to start a listener until you have properly setup your protocol
   * routes, since you might miss messages between the time the listener is <code>start()</code> 'ed and the time you add your routes.
   * 
   * @throws IOException if an IO error occurs (this will most likely be a problem binding to the specified port/address)
   */
  @Override
  public synchronized void start(Set<ConnectionID> initialConnectionIDs) throws IOException {
    this.lsnr = this.commsMgr.createCommsListener(this.addr, this.channelManager, this.reuseAddr, initialConnectionIDs,
                                                  this.connectionIdFactory, this.wireProtoMsgSnk);
    this.started = true;
    this.commsMgr.registerListener(this);
  }

  @Override
  public synchronized void stop(long timeout) throws TCTimeoutException {
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

  @Override
  public ChannelManager getChannelManager() {
    return this.channelManager;
  }

  @Override
  public synchronized InetAddress getBindAddress() {
    if (!this.started) { throw new IllegalStateException("Listener not running"); }
    return this.lsnr.getBindAddress();
  }

  @Override
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
