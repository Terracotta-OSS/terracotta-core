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
package com.tc.net.protocol.tcm;

import com.tc.net.core.TCListener;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
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
  private final InetSocketAddress addr;
  private TCListener lsnr;
  private CompletableFuture<Boolean> started = null;
  private final boolean reuseAddr;
  private final ConnectionIDFactory connectionIdFactory;
  private final WireProtocolMessageSink wireProtoMsgSnk;
  private final RedirectAddressProvider activeProvider;
  private final Predicate<MessageTransport> validation;

  // this constructor is intentionally not public, only the Comms Manager should be creating them
  NetworkListenerImpl(InetSocketAddress addr, CommunicationsManagerImpl commsMgr, ChannelManagerImpl channelManager,
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
    if (startDone == null) {
      LOGGER.info("network listener already started");
    } else {
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

  @Override
  public boolean isStarted() {
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
    if (!isStarted()) {
      return this.addr.getAddress();
    } else {
      return this.lsnr.getBindSocketAddress().getAddress();
    }
  }

  @Override
  public synchronized int getBindPort() {
    if (!isStarted()) {
      return this.addr.getPort();
    } else {
      return this.lsnr.getBindSocketAddress().getPort();
    }
  }

  @Override
  public String toString() {
    try {
      NetworkInterface eth = NetworkInterface.getByInetAddress(addr.getAddress());
      if (eth != null) {
        return "interface:" + eth.getDisplayName() + " (address:" + addr.getAddress() + " port:" + addr.getPort() +')';
      } else {
        return "all interfaces (address:" + addr.getAddress() + " port:" + addr.getPort() +')';
      }
    } catch (final Exception e) {
      return "Exception in toString(): " + e.getMessage();
    }
  }
}
