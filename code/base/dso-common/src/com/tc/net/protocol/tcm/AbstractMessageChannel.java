/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author teck
 */
abstract class AbstractMessageChannel implements MessageChannel, MessageChannelInternal {

  private final Map               attachments    = new ConcurrentReaderHashMap();
  private final Object            attachmentLock = new Object();
  private final Set               listeners      = new CopyOnWriteArraySet();
  private final ChannelStatus     status         = new ChannelStatus();
  private final TCMessageFactory  msgFactory;
  private final TCMessageRouter   router;
  private final TCMessageParser   parser;
  private final TCLogger          logger;
  private final NodeID            remoteNodeID;
  private volatile NodeID         localNodeID;

  protected volatile NetworkLayer sendLayer;

  AbstractMessageChannel(TCMessageRouter router, TCLogger logger, TCMessageFactory msgFactory, NodeID remoteNodeID) {
    this.router = router;
    this.logger = logger;
    this.msgFactory = msgFactory;
    this.parser = new TCMessageParser(this.msgFactory);
    this.remoteNodeID = remoteNodeID;
    // This is set after hand shake for the clients
    this.localNodeID = ClientID.NULL_ID;
  }

  public void addAttachment(String key, Object value, boolean replace) {
    synchronized (attachmentLock) {
      boolean exists = attachments.containsKey(key);
      if (replace || !exists) {
        attachments.put(key, value);
      }
    }
  }

  public Object removeAttachment(String key) {
    return this.attachments.remove(key);
  }

  public Object getAttachment(String key) {
    return this.attachments.get(key);
  }

  public boolean isOpen() {
    return this.status.isOpen();
  }

  public boolean isClosed() {
    return this.status.isClosed();
  }

  public void addListener(ChannelEventListener listener) {
    if (listener == null) { return; }

    listeners.add(listener);
  }

  public NodeID getLocalNodeID() {
    return localNodeID;
  }

  public void setLocalNodeID(NodeID localNodeID) {
    this.localNodeID = localNodeID;
  }

  public NodeID getRemoteNodeID() {
    return remoteNodeID;
  }

  public TCMessage createMessage(TCMessageType type) {
    TCMessage rv = this.msgFactory.createMessage(this, type);
    // TODO: set default channel specific information in the TC message header

    return rv;
  }

  public abstract NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException,
      UnknownHostException, IOException, CommStackMismatchException;

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

  public void close() {
    if (!status.getAndSetIsClosed()) {
      Assert.assertNotNull(this.sendLayer);
      this.sendLayer.close();
      fireChannelClosedEvent();
    }
  }

  public boolean isConnected() {
    return this.sendLayer != null && this.sendLayer.isConnected();
  }

  public final void setSendLayer(NetworkLayer layer) {
    this.sendLayer = layer;
  }

  public final void setReceiveLayer(NetworkLayer layer) {
    throw new UnsupportedOperationException();
  }

  public NetworkLayer getReceiveLayer() {
    // this is the topmost layer, it has no parent
    return null;
  }

  public void send(final TCNetworkMessage message) {
    if (logger.isDebugEnabled()) {
      final Runnable logMsg = new Runnable() {
        public void run() {
          logger.debug("Message Sent: " + message.toString());
        }
      };

      final Runnable existingCallback = message.getSentCallback();
      final Runnable newCallback;

      if (existingCallback != null) {
        newCallback = new Runnable() {
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

  public final void receive(TCByteBuffer[] msgData) {
    this.router.putMessage(parser.parseMessage(this, msgData));
  }

  protected final ChannelStatus getStatus() {
    return status;
  }

  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    fireTransportDisconnectedEvent();
  }

  protected void fireTransportDisconnectedEvent() {
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_DISCONNECTED_EVENT, AbstractMessageChannel.this));
  }

  public void notifyTransportConnected(MessageTransport transport) {
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_CONNECTED_EVENT, AbstractMessageChannel.this));
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    return;
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // yeah, we know. We closed it.
    return;
  }

  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_RECONNECTION_REJECTED_EVENT, AbstractMessageChannel.this));
  }

  public TCSocketAddress getLocalAddress() {
    NetworkLayer sendLyr = this.sendLayer;
    if (sendLyr != null) {
      return sendLyr.getLocalAddress();
    } else {
      return null;
    }
  }

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
  public short getStackLayerFlag() {
    // this is the channel layer
    return TYPE_CHANNEL_LAYER;
  }

  /**
   * this function gets the stack Layer Name added to build the communctaion stack information
   */
  public String getStackLayerName() {
    // this is the channel layer
    return NAME_CHANNEL_LAYER;
  }

  @Override
  public String toString() {
    return ((isOpen() ? getChannelID() : "ChannelID[NULL_ID, " + getStatus() + "]") + ":" + getLocalAddress()
            + " <--> " + getRemoteAddress());
  }

  class ChannelStatus {
    private ChannelState state;

    public ChannelStatus() {
      this.state = ChannelState.INIT;
    }

    synchronized void open() {
      Assert.assertTrue("Switch only from init state to open state", ChannelState.INIT.equals(state));
      state = ChannelState.OPEN;
    }

    synchronized boolean getAndSetIsClosed() {
      if (ChannelState.INIT.equals(state)) {
        logger.warn("Ignoring state switch to " + ChannelState.CLOSED + "; Current State : " + state + "; "
                    + AbstractMessageChannel.this);
        return false;
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

  private static class ChannelState {
    private static final int  STATE_INIT   = 0;
    private static final int  STATE_OPEN   = 1;
    private static final int  STATE_CLOSED = 2;

    static final ChannelState INIT         = new ChannelState(STATE_INIT);
    static final ChannelState OPEN         = new ChannelState(STATE_OPEN);
    static final ChannelState CLOSED       = new ChannelState(STATE_CLOSED);

    private final int         state;

    private ChannelState(int state) {
      this.state = state;
    }

    @Override
    public String toString() {
      switch (state) {
        case STATE_INIT:
          return "INIT";
        case STATE_OPEN:
          return "OPEN";
        case STATE_CLOSED:
          return "CLOSED";
        default:
          return "UNKNOWN";
      }
    }
  }

  // for testing purpose
  protected NetworkLayer getSendLayer() {
    return sendLayer;
  }

}
