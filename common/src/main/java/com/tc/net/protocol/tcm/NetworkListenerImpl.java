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

import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handle to a specific server port listener
 * 
 * @author teck
 */
class NetworkListenerImpl implements NetworkListener {
  private static Logger LOGGER = LoggerFactory.getLogger(NetworkListenerImpl.class);

  private final ChannelManagerImpl channelManager;
  private final CommunicationsManagerImpl commsMgr;
  private final TCSocketAddress addr;
  private TCListener lsnr;
  private CompletableFuture<Boolean> started = null;
  private final boolean reuseAddr;
  private final ConnectionIDFactory connectionIdFactory;
  private final WireProtocolMessageSink wireProtoMsgSnk;
  private final RedirectAddressProvider activeProvider;
  private final Predicate<MessageTransport> validation;

  // this constructor is intentionally not public, only the Comms Manager should be creating them
  NetworkListenerImpl(TCSocketAddress addr, CommunicationsManagerImpl commsMgr, ChannelManagerImpl channelManager,
                      TCMessageFactory msgFactory, boolean reuseAddr, ConnectionIDFactory connectionIdFactory,
                      WireProtocolMessageSink wireProtoMsgSnk, RedirectAddressProvider activeProvider, Predicate<MessageTransport> validation) {
    this.commsMgr = commsMgr;
    this.channelManager = channelManager;
    this.addr = addr;
    this.connectionIdFactory = connectionIdFactory;
    this.wireProtoMsgSnk = wireProtoMsgSnk;
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
  public void start(Set<ConnectionID> initialConnectionIDs) throws IOException {
    CompletableFuture<Boolean> startDone = start();
    try {
      this.lsnr = this.commsMgr.createCommsListener(this.addr, this.channelManager, this.reuseAddr, initialConnectionIDs, this.activeProvider,
                                                  this.validation, this.connectionIdFactory, this.wireProtoMsgSnk);
      this.commsMgr.registerListener(this);
      startDone.complete(true);
    } catch (IOException ioe) {
      startDone.completeExceptionally(ioe);
      throw ioe;
    } finally {
      if (!startDone.isDone()) {
        startDone.complete(false);
      }
    }
  }

  @Override
  public void stop(long timeout) throws TCTimeoutException {
    Future<Boolean> startDone = stop();
    boolean stop;
    try {
      if (timeout == 0L) {
        stop = startDone.get();
      } else {
        long time = System.currentTimeMillis();
        stop = startDone.get(timeout, TimeUnit.MILLISECONDS);
        timeout -= System.currentTimeMillis() - time;
        if (timeout <= 0L) {
          throw new TCTimeoutException("unable to stop network listener in time alloted");
        }
      }
      if (stop) {
        this.lsnr.stop(timeout);
        this.commsMgr.unregisterListener(this);
      }
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.warn("listener not stopped", e);
    } catch (TimeoutException to) {
      throw new TCTimeoutException(to);
    }
  }

  private synchronized CompletableFuture<Boolean> start() {
    if (started == null) {
      started = new CompletableFuture<>();
      return started;
    } else {
      return null;
    }
  }

  private synchronized CompletableFuture<Boolean> stop() {
    if (started == null) {
      started = CompletableFuture.completedFuture(false);
    }
    return started;
  }

  private boolean isStarted() {
    Future<Boolean> startDone = null;
    synchronized (this) {
      if (started == null) {
        return false;
      } else {
        startDone = started;
      }
    }
    try {
      return startDone.get();
    } catch (ExecutionException | InterruptedException e) {
      return false;
    }
  }

  @Override
  public ChannelManager getChannelManager() {
    return this.channelManager;
  }

  @Override
  public synchronized InetAddress getBindAddress() {
    if (!isStarted()) { throw new IllegalStateException("Listener not running"); }
    return this.lsnr.getBindAddress();
  }

  @Override
  public synchronized int getBindPort() {
    if (!isStarted()) { throw new IllegalStateException("Listener not running"); }
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
