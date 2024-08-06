/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.tc.io.TCByteBufferOutputStream;
import org.slf4j.Logger;

import com.tc.net.ClientID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.core.ProductID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author teck
 */
abstract class AbstractMessageChannel implements MessageChannelInternal {

  private final ConcurrentMap<String, Object> attachments = new ConcurrentHashMap<String, Object>();
  private final Set<ChannelEventListener>     listeners   = new CopyOnWriteArraySet<ChannelEventListener>();
  private final ChannelStatus                 status      = new ChannelStatus();
  private final TCMessageFactory              msgFactory;
  private final TCMessageRouter               router;
  private final TCMessageParser               parser;
  private final Logger logger;
  private volatile NodeID                     localNodeID;

  protected volatile NetworkLayer             sendLayer;

  AbstractMessageChannel(TCMessageRouter router, Logger logger, TCMessageFactory msgFactory) {
    this.router = router;
    this.logger = logger;
    this.msgFactory = msgFactory;
    this.parser = new TCMessageParser(this.msgFactory);
    // This is set after hand shake for the clients
    this.localNodeID = ClientID.NULL_ID;
  }

  @Override
  public NetworkStackID open(InetSocketAddress serverAddress) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException {
    return open(Collections.singleton(serverAddress));
  }

  @Override
  public void addAttachment(String key, Object value, boolean replace) {
    if (replace) {
      attachments.put(key, value);
    } else {
      attachments.putIfAbsent(key, value);
    }
  }

  @Override
  public Object removeAttachment(String key) {
    return this.attachments.remove(key);
  }

  @Override
  public Object getAttachment(String key) {
    return this.attachments.get(key);
  }

  @Override
  public boolean isOpen() {
    return this.status.isOpen();
  }

  @Override
  public boolean isClosed() {
    return this.status.isClosed();
  }

  @Override
  public void addListener(ChannelEventListener listener) {
    if (listener == null) { return; }
    listeners.add(listener);
  }

  @Override
  public NodeID getLocalNodeID() {
    return localNodeID;
  }

  @Override
  public void setLocalNodeID(NodeID localNodeID) {
    this.localNodeID = localNodeID;
  }

  @Override
  public TCAction createMessage(TCMessageType type) {
    TCAction rv = this.msgFactory.createMessage(this, type, createOutput());
    // TODO: set default channel specific information in the TC message header

    return rv;
  }

  @Override
  public TCByteBufferOutputStream createOutput() {
    return sendLayer.createOutput();
  }
  
  private void fireChannelOpenedEvent() {
    fireEvent(new ChannelEventImpl(ChannelEventType.CHANNEL_OPENED_EVENT, AbstractMessageChannel.this));
  }

  private void fireChannelClosedEvent() {
    fireEvent(new ChannelEventImpl(ChannelEventType.CHANNEL_CLOSED_EVENT, AbstractMessageChannel.this));
  }

  public void addClassMapping(TCMessageType type, Class<? extends TCAction> msgClass) {
    this.msgFactory.addClassMapping(type, msgClass);
  }

  void channelOpened() {
    status.open();
    fireChannelOpenedEvent();
  }

  @Override
  public void close() {
    if (!status.getAndSetIsClosed()) {
      Assert.assertNotNull(this.sendLayer);
      this.sendLayer.close();
      fireChannelClosedEvent();
    }
  }

  @Override
  public boolean isConnected() {
    return this.sendLayer != null && this.sendLayer.isConnected();
  }

  @Override
  public final void setSendLayer(NetworkLayer layer) {
    this.sendLayer = layer;
  }

  @Override
  public final void setReceiveLayer(NetworkLayer layer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkLayer getReceiveLayer() {
    // this is the topmost layer, it has no parent
    return null;
  }

  @Override
  public void send(final TCNetworkMessage message) throws IOException {
    if (logger.isDebugEnabled()) {
      message.addCompleteCallback(()->logger.debug("Message Sent: " + message.toString()));
    }

    this.sendLayer.send(message);
  }

  @Override
  public final void receive(TCNetworkMessage msgData) {
    this.router.putMessage(parser.parseMessage(this, msgData));
  }

  protected final ChannelStatus getStatus() {
    return status;
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_DISCONNECTED_EVENT, AbstractMessageChannel.this));
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_CONNECTED_EVENT, AbstractMessageChannel.this));
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    return;
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    // yeah, we know. We closed it.
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_CLOSED_EVENT, AbstractMessageChannel.this));
    return;
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_RECONNECTION_REJECTED_EVENT, AbstractMessageChannel.this));
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    NetworkLayer sendLyr = this.sendLayer;
    if (sendLyr != null) {
      return sendLyr.getLocalAddress();
    } else {
      return null;
    }
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    NetworkLayer sendLyr = this.sendLayer;
    if (sendLyr != null) {
      return sendLyr.getRemoteAddress();
    } else {
      return null;
    }
  }

  private void fireEvent(ChannelEventImpl event) {
    for (ChannelEventListener listener : listeners) {
      listener.notifyChannelEvent(event);
    }
  }

  /**
   * this function gets the stack Lyaer Flag added to build the communctaion stack information
   */
  @Override
  public short getStackLayerFlag() {
    // this is the channel layer
    return TYPE_CHANNEL_LAYER;
  }

  /**
   * this function gets the stack Layer Name added to build the communctaion stack information
   */
  @Override
  public String getStackLayerName() {
    // this is the channel layer
    return NAME_CHANNEL_LAYER;
  }

  @Override
  public String toString() {
    return (getChannelID() + ":" + getLocalAddress() + " <--> " + getRemoteAddress());
  }

  protected ProductID getProductID(ProductID defaultID) {
    if (this.sendLayer != null) {
      return this.sendLayer.getConnectionID().getProductId();
    } else {
      return defaultID;
    }
  }

  @Override
  public ConnectionID getConnectionID() {
    if (this.sendLayer != null) {
      return this.sendLayer.getConnectionID();
    } else {
      return ConnectionID.NULL_ID;
    }
  }

  @Override
  public ChannelID getChannelID() {
    return new ChannelID(getConnectionID().getChannelID());
  }

  private enum ChannelState {
    INIT, OPEN, CLOSED
  }

  class ChannelStatus {
    private ChannelState state;

    public ChannelStatus() {
      this.state = ChannelState.INIT;
    }

    synchronized void reset() {
      this.state = ChannelState.INIT;
    }

    synchronized void open() {
      Assert.assertTrue("Switch only from init state to open state", ChannelState.INIT.equals(state));
      state = ChannelState.OPEN;
    }

    synchronized boolean getAndSetIsClosed() {
      if (ChannelState.INIT.equals(state)) {
        // Treating an unopened channel as effectively equivalent to closed.
        logger.debug("Switching channel state from " + ChannelState.INIT + " to " + ChannelState.CLOSED + ".");
        state = ChannelState.CLOSED;
        return true;
      }

      if (ChannelState.CLOSED.equals(state)) {
        return true;
      } else {
        state = ChannelState.CLOSED;
        return false;
      }
    }

    synchronized boolean isOpen() {
      return ChannelState.OPEN.equals(state);
    }

    synchronized boolean isClosed() {
      return ChannelState.CLOSED.equals(state);
    }

    @Override
    public String toString() {
      return "Status:" + this.state.toString();
    }

  }

  @Override
  public SessionID getSessionID() {
    return sendLayer.getSessionID();
  }

  // for testing purpose
  protected NetworkLayer getSendLayer() {
    return sendLayer;
  }

}
