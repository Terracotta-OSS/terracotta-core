/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.net.MaxConnectionsExceededException;
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
  
  private final Map                     attachments               = new ConcurrentReaderHashMap();
  private final Object                  attachmentLock            = new Object();
  private final Set                     listeners                 = new CopyOnWriteArraySet();
  private final ChannelStatus           status                    = new ChannelStatus();
  private final SynchronizedRef         remoteAddr                = new SynchronizedRef(null);
  private final SynchronizedRef         localAddr                 = new SynchronizedRef(null);
  private final TCMessageFactory        msgFactory;
  private final TCMessageRouter         router;
  private final TCMessageParser         parser;
  private final TCLogger                logger;

  protected NetworkLayer                sendLayer;

  AbstractMessageChannel(TCMessageRouter router, TCLogger logger, TCMessageFactory msgFactory) {
    this.router = router;
    this.logger = logger;
    this.msgFactory = msgFactory;
    this.parser = new TCMessageParser(this.msgFactory);
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

  public TCMessage createMessage(TCMessageType type) {
    TCMessage rv = this.msgFactory.createMessage(this, type);
    // TODO: set default channel specific information in the TC message header

    return rv;
  }

  public void routeMessageType(TCMessageType messageType, TCMessageSink dest) {
    router.routeMessageType(messageType, dest);
  }

  public void unrouteMessageType(TCMessageType messageType) {
    router.unrouteMessageType(messageType);
  }

  public abstract NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException;

  /**
   * Routes a TCMessage to a sink. The hydrate sink will do the hydrate() work
   */
  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
    routeMessageType(messageType, new TCMessageSinkToSedaSink(destSink, hydrateSink));
  }

  public void close() {
    synchronized (status) {
      if (!status.isClosed()) {
        Assert.assertNotNull(this.sendLayer);
        this.sendLayer.close();
      }
      status.close();
    }
  }

  public final boolean isConnected() {
    return this.sendLayer != null && this.sendLayer.isConnected();
  }

  public final void setSendLayer(NetworkLayer layer) {
    this.sendLayer = layer;
  }

  public final void setReceiveLayer(NetworkLayer layer) {
    throw new UnsupportedOperationException();
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

  public void notifyTransportDisconnected(MessageTransport transport) {
    this.remoteAddr.set(null);
    this.localAddr.set(null);
    fireTransportDisconnectedEvent();
  }

  protected void fireTransportDisconnectedEvent() {
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_DISCONNECTED_EVENT, AbstractMessageChannel.this));
  }

  public void notifyTransportConnected(MessageTransport transport) {
    this.remoteAddr.set(transport.getRemoteAddress());
    this.localAddr.set(transport.getLocalAddress());
    fireEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_CONNECTED_EVENT, AbstractMessageChannel.this));
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    return;
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // yeah, we know. We closed it.
    return;
  }

  public TCSocketAddress getLocalAddress() {
    return (TCSocketAddress) this.localAddr.get();
  }

  public TCSocketAddress getRemoteAddress() {
    return (TCSocketAddress) this.remoteAddr.get();
  }

  private void fireEvent(ChannelEventImpl event) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      ((ChannelEventListener) i.next()).notifyChannelEvent(event);
    }
  }

  class ChannelStatus {
    private ChannelState state;

    public ChannelStatus() {
      this.state = ChannelState.CLOSED;
    }

    // this method non-public on purpose. Only the channel should change it's own status
    synchronized void close() {
      changeState(ChannelState.CLOSED);
      fireEvent(new ChannelEventImpl(ChannelEventType.CHANNEL_CLOSED_EVENT, AbstractMessageChannel.this));
    }

    // this method non-public on purpose. Only the channel should change it's own status
    synchronized void open() {
      changeState(ChannelState.OPEN);
      fireEvent(new ChannelEventImpl(ChannelEventType.CHANNEL_OPENED_EVENT, AbstractMessageChannel.this));
    }

    synchronized boolean isOpen() {
      return ChannelState.OPEN.equals(state);
    }

    synchronized boolean isClosed() {
      return ChannelState.CLOSED.equals(state);
    }

    private synchronized void changeState(ChannelState newState) {
      state = newState;
    }
  }

  private static class ChannelState {
    private static final int  STATE_OPEN   = 1;
    private static final int  STATE_CLOSED = 2;

    static final ChannelState OPEN         = new ChannelState(STATE_OPEN);
    static final ChannelState CLOSED       = new ChannelState(STATE_CLOSED);

    private final int         state;

    private ChannelState(int state) {
      this.state = state;
    }

    public String toString() {
      switch (state) {
        case STATE_OPEN:
          return "OPEN";
        case STATE_CLOSED:
          return "CLOSED";
        default:
          return "UNKNOWN";
      }
    }
  }
}