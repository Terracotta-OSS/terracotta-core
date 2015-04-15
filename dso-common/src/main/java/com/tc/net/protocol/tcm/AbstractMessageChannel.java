/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.license.ProductID;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.util.Assert;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author teck
 */
abstract class AbstractMessageChannel implements MessageChannel, MessageChannelInternal {

  private final ConcurrentMap<String, Object> attachments    = new ConcurrentHashMap<String, Object>();
  private final Set               listeners      = new CopyOnWriteArraySet();
  private final ChannelStatus     status         = new ChannelStatus();
  private final TCMessageFactory  msgFactory;
  private final ProductID productId;
  private final TCMessageRouter   router;
  private final TCMessageParser   parser;
  private final TCLogger          logger;
  private final NodeID            remoteNodeID;
  private volatile NodeID         localNodeID;

  protected volatile NetworkLayer sendLayer;

  AbstractMessageChannel(TCMessageRouter router, TCLogger logger, TCMessageFactory msgFactory, NodeID remoteNodeID,
                         ProductID productId) {
    this.router = router;
    this.logger = logger;
    this.msgFactory = msgFactory;
    this.productId = productId;
    this.parser = new TCMessageParser(this.msgFactory);
    this.remoteNodeID = remoteNodeID;
    // This is set after hand shake for the clients
    this.localNodeID = ClientID.NULL_ID;
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
  public NodeID getRemoteNodeID() {
    return remoteNodeID;
  }

  @Override
  public TCMessage createMessage(TCMessageType type) {
    TCMessage rv = this.msgFactory.createMessage(this, type);
    // TODO: set default channel specific information in the TC message header

    return rv;
  }

  private void fireChannelOpenedEvent() {
    fireEvent(new ChannelEventImpl(ChannelEventType.CHANNEL_OPENED_EVENT, AbstractMessageChannel.this));
  }

  private void fireChannelClosedEvent() {
    fireEvent(new ChannelEventImpl(ChannelEventType.CHANNEL_CLOSED_EVENT, AbstractMessageChannel.this));
  }

  public void addClassMapping(final TCMessageType type, final Class msgClass) {
    this.msgFactory.addClassMapping(type, msgClass);
  }

  public void addClassMapping(final TCMessageType type, final GeneratedMessageFactory messageFactory) {
    this.msgFactory.addClassMapping(type, messageFactory);
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
  public void send(final TCNetworkMessage message) {
    if (logger.isDebugEnabled()) {
      final Runnable logMsg = new Runnable() {
        @Override
        public void run() {
          logger.debug("Message Sent: " + message.toString());
        }
      };

      final Runnable existingCallback = message.getSentCallback();
      final Runnable newCallback;

      if (existingCallback != null) {
        newCallback = new Runnable() {
          @Override
          public void run() {
            try {
              existingCallback.run();
            } catch (Exception e) {
              logger.error(e);
            } finally {
              logMsg.run();
            }
          }
        };
      } else {
        newCallback = logMsg;
      }

      message.setSentCallback(newCallback);
    }

    this.sendLayer.send(message);
  }

  @Override
  public final void receive(TCByteBuffer[] msgData) {
    this.router.putMessage(parser.parseMessage(this, msgData));
  }

  protected final ChannelStatus getStatus() {
    return status;
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
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
    return;
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_RECONNECTION_REJECTED_EVENT, AbstractMessageChannel.this));
  }

  @Override
  public TCSocketAddress getLocalAddress() {
    NetworkLayer sendLyr = this.sendLayer;
    if (sendLyr != null) {
      return sendLyr.getLocalAddress();
    } else {
      return null;
    }
  }

  @Override
  public TCSocketAddress getRemoteAddress() {
    NetworkLayer sendLyr = this.sendLayer;
    if (sendLyr != null) {
      return sendLyr.getRemoteAddress();
    } else {
      return null;
    }
  }

  private void fireEvent(ChannelEventImpl event) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      ((ChannelEventListener) i.next()).notifyChannelEvent(event);
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
    return ((isOpen() ? getChannelID() : "ChannelID[NULL_ID, " + getStatus() + "]") + ":" + getLocalAddress()
            + " <--> " + getRemoteAddress());
  }

  @Override
  public ProductID getProductId() {
    return productId;
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
        logger.warn("Switcing channel state from " + ChannelState.INIT + " to " + ChannelState.CLOSED + ".");
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

  // for testing purpose
  protected NetworkLayer getSendLayer() {
    return sendLayer;
  }

}
