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
package com.tc.net.protocol.tcm;

import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.operatorevent.NodeNameProvider;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.function.Predicate;

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
  private final NodeNameProvider activeProvider;
  private final Predicate<MessageTransport> validation;

  // this constructor is intentionally not public, only the Comms Manager should be creating them
  NetworkListenerImpl(TCSocketAddress addr, CommunicationsManagerImpl commsMgr, ChannelManagerImpl channelManager,
                      TCMessageFactory msgFactory, boolean reuseAddr, ConnectionIDFactory connectionIdFactory,
                      WireProtocolMessageSink wireProtoMsgSnk, NodeNameProvider activeProvider, Predicate<MessageTransport> validation) {
    this.commsMgr = commsMgr;
    this.channelManager = channelManager;
    this.addr = addr;
    this.connectionIdFactory = connectionIdFactory;
    this.wireProtoMsgSnk = wireProtoMsgSnk;
    this.started = false;
    this.reuseAddr = reuseAddr;
    this.activeProvider = activeProvider;
    this.validation = validation;
  }

  /**
   * Start this listener listening on the network. You probably don't want to start a listener until you have properly setup your protocol
   * routes, since you might miss messages between the time the listener is <code>start()</code> 'ed and the time you add your routes.
   * 
   * @throws IOException if an IO error occurs (this will most likely be a problem binding to the specified port/address)
   */
  @Override
  public synchronized void start(Set<ClientID> initialConnectionIDs) throws IOException {
    this.lsnr = this.commsMgr.createCommsListener(this.addr, this.channelManager, this.reuseAddr, initialConnectionIDs, this.activeProvider,
                                                  this.validation, this.connectionIdFactory, this.wireProtoMsgSnk);
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
