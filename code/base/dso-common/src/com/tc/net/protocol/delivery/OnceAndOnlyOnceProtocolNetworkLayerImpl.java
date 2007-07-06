/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolException;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.transport.AbstractMessageTransport;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.WireProtocolMessage;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * NetworkLayer implementation for once and only once message delivery protocol.
 */
public class OnceAndOnlyOnceProtocolNetworkLayerImpl extends AbstractMessageTransport implements
    OnceAndOnlyOnceProtocolNetworkLayer, OOOProtocolMessageDelivery {
  private static final TCLogger           logger        = TCLogging
                                                            .getLogger(OnceAndOnlyOnceProtocolNetworkLayerImpl.class);
  private final OOOProtocolMessageFactory messageFactory;
  private final OOOProtocolMessageParser  messageParser;
  boolean                                 wasConnected  = false;
  private MessageChannelInternal          receiveLayer;
  private MessageTransport                sendLayer;
  private GuaranteedDeliveryProtocol      delivery;
  private final SynchronizedBoolean       reconnectMode = new SynchronizedBoolean(false);
  private final SynchronizedBoolean       handshakeMode = new SynchronizedBoolean(false);
  private final SynchronizedBoolean       channelConnected = new SynchronizedBoolean(false);
  private boolean                         isClosed      = false;
  private final boolean                   isClient;
  private final String                    debugId;
  private short                           sessionId     = -1;
  private static final boolean            debug         = false;

  public OnceAndOnlyOnceProtocolNetworkLayerImpl(OOOProtocolMessageFactory messageFactory,
                                                 OOOProtocolMessageParser messageParser, Sink workSink, boolean isClient) {
    super(logger);
    this.messageFactory = messageFactory;
    this.messageParser = messageParser;
    this.isClient = isClient;
    this.delivery = new GuaranteedDeliveryProtocol(this, workSink, isClient);
    this.delivery.start();
    this.delivery.pause();
    this.sessionId = (this.isClient) ? -1 : newRandomSessionId();
    this.debugId = (this.isClient) ? "CLIENT" : "SERVER";
  }

  /*********************************************************************************************************************
   * Network layer interface...
   */

  public void setSendLayer(NetworkLayer layer) {
    if (!(layer instanceof MessageTransport)) { throw new IllegalArgumentException(
                                                                                   "Error: send layer must be MessageTransport!"); }
    this.setSendLayer((MessageTransport) layer);
  }

  public void setSendLayer(MessageTransport transport) {
    this.sendLayer = transport;
  }

  public void setReceiveLayer(NetworkLayer layer) {
    if (!(layer instanceof MessageChannelInternal)) { throw new IllegalArgumentException(
                                                                                         "Error: receive layer must be MessageChannelInternal, was "
                                                                                             + layer.getClass()
                                                                                                 .getName()); }
    this.receiveLayer = (MessageChannelInternal) layer;
  }

  public void send(TCNetworkMessage message) {
    delivery.send(message);
  }

  public void receive(TCByteBuffer[] msgData) {
    OOOProtocolMessage msg = createProtocolMessage(msgData);
    debugLog("receive -> " + msg.getHeader().toString());
    if (msg.isSend()) {
      Assert.inv(!handshakeMode.get());
      Assert.inv(channelConnected.get());
      delivery.receive(msg);
    } else if (msg.isAck()) {
      Assert.inv(!handshakeMode.get());
      Assert.inv(channelConnected.get());
      delivery.receive(msg);
    } else if (msg.isHandshake()) {
      Assert.inv(!isClient);
      debugLog("Got Handshake message...");
      if (msg.getSessionId() == -1) {
        debugLog("A brand new client is trying to connect - reply OK");
        OOOProtocolMessage reply = createHandshakeReplyOkMessage(delivery.getReceiver().getReceived().get());
        sendMessage(reply);
        delivery.resume();
        delivery.receive(createHandshakeReplyOkMessage(-1));
        handshakeMode.set(false);
        if (!channelConnected.get()) receiveLayer.notifyTransportConnected(this);
        channelConnected.set(true);
        reconnectMode.set(false);
      } else if (msg.getSessionId() == getSessionId()) {
        debugLog("A same-session client is trying to connect - reply OK");
        OOOProtocolMessage reply = createHandshakeReplyOkMessage(delivery.getReceiver().getReceived().get());
        sendMessage(reply);
        handshakeMode.set(false);
        delivery.resume();
        // tell local sender the ackseq of client
        delivery.receive(createHandshakeReplyOkMessage(msg.getAckSequence()));
        if (!channelConnected.get()) receiveLayer.notifyTransportConnected(this);
        channelConnected.set(true);
        reconnectMode.set(false);
      } else {
        debugLog("A DIFF-session client is trying to connect - reply FAIL");
        OOOProtocolMessage reply = createHandshakeReplyFailMessage(delivery.getReceiver().getReceived().get());
        sendMessage(reply);
        handshakeMode.set(false);     
        if (channelConnected.get()) receiveLayer.notifyTransportDisconnected(this);
        resetStack();
        delivery.resume();
        delivery.receive(reply);
        receiveLayer.notifyTransportConnected(this);
        channelConnected.set(true);
        reconnectMode.set(false);
      }
    } else if (msg.isHandshakeReplyOk()) {
      Assert.inv(isClient);
      Assert.inv(handshakeMode.get());
      debugLog("Got reply OK");
      // current session is still ok:
      // 1. might have to resend some messages
      // 2. no need to signal to Higher Level
      handshakeMode.set(false);
      sessionId = msg.getSessionId();
      delivery.resume();
      delivery.receive(msg);
      if (!channelConnected.get()) receiveLayer.notifyTransportConnected(this);
      channelConnected.set(true);
      reconnectMode.set(false);
    } else if (msg.isHandshakeReplyFail()) {
      debugLog("Received handshake fail reply");
      Assert.inv(isClient);
      Assert.inv(handshakeMode.get());
      // we did not synch'ed the existing session.
      // 1. clear OOO state (drop messages, clear counters, etc)
      // 2. set the new session
      // 3. signal Higher Lever to re-synch
      if (channelConnected.get()) receiveLayer.notifyTransportDisconnected(this);
      channelConnected.set(false);
      resetStack();
      sessionId = msg.getSessionId();
      handshakeMode.set(false);
      delivery.resume();
      delivery.receive(msg);
      if (!channelConnected.get()) receiveLayer.notifyTransportConnected(this);
      channelConnected.set(true);
      reconnectMode.set(false);
    } else if (msg.isGoodbye()) {
      debugLog("Got GoodBye message - shutting down");
      isClosed = true;
      sendLayer.close();
      receiveLayer.close();
      delivery.pause();
    } else {
      Assert.inv(false);
    }
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("OOOLayer-" + debugId + "-" + sendLayer.getConnectionId() + " -> " + msg);
    }
  }

  public boolean isConnected() {
    Assert.assertNotNull(sendLayer);
    return sendLayer.isConnected();
  }

  public NetworkStackID open() throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException {
    Assert.assertNotNull(sendLayer);
    return sendLayer.open();
  }

  public void close() {
    Assert.assertNotNull(sendLayer);
    // send goobye message with session-id on it
    OOOProtocolMessage opm = messageFactory.createNewGoodbyeMessage(getSessionId());
    sendLayer.send(opm);
    sendLayer.close();
  }

  /*********************************************************************************************************************
   * Transport listener interface...
   */

  public void notifyTransportConnected(MessageTransport transport) {
    handshakeMode.set(true);
    if (isClient) {
      OOOProtocolMessage handshake = createHandshakeMessage(delivery.getReceiver().getReceived().get());
      debugLog("Sending Handshake message...");
      sendMessage(handshake);
    } else {
      // resue for missing transportDisconnected events
      if (!delivery.isPaused()) {
        notifyTransportDisconnected(null);
      }
    }
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    final boolean restoreConnectionMode = reconnectMode.get();
    debugLog("Transport Disconnected - pausing delivery, restoreConnection = " + restoreConnectionMode);
    this.delivery.pause();
    if (!restoreConnectionMode) {
    	if(channelConnected.get())receiveLayer.notifyTransportDisconnected(this);
    	channelConnected.set(false);
    }
    reconnectMode.set(false);
  }

  public void start() {
    //
  }

  public void pause() {
    this.delivery.pause();
  }

  public void resume() {
    this.delivery.resume();
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    if (!reconnectMode.get()) {
      receiveLayer.notifyTransportConnectAttempt(this);
    }
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // XXX: do we do anything here? We've probably done everything we need to do when close() was called.
    debugLog("Transport Closed - notifying higher layer");
    receiveLayer.notifyTransportClosed(this);
    channelConnected.set(false);
  }

  /*********************************************************************************************************************
   * Protocol Message Delivery interface
   */

  public OOOProtocolMessage createHandshakeMessage(long ack) {
    OOOProtocolMessage rv = this.messageFactory.createNewHandshakeMessage(getSessionId(), ack);
    return rv;
  }

  public OOOProtocolMessage createHandshakeReplyOkMessage(long ack) {
    // FIXME: need to use correct ack
    OOOProtocolMessage rv = this.messageFactory.createNewHandshakeReplyOkMessage(getSessionId(), ack);
    return rv;
  }

  public OOOProtocolMessage createHandshakeReplyFailMessage(long ack) {
    // FIXME: need to use correct ack
    OOOProtocolMessage rv = this.messageFactory.createNewHandshakeReplyFailMessage(getSessionId(), ack);
    return rv;
  }

  private short getSessionId() {
    return sessionId;
  }

  public OOOProtocolMessage createAckMessage(long ack) {
    return (this.messageFactory.createNewAckMessage(getSessionId(), ack));
  }

  public boolean sendMessage(OOOProtocolMessage msg) {
    // this method doesn't do anything at the moment, but it is a good spot to plug in things you might want to do
    // every message flowing down from the layer (like logging for example)
    if (this.sendLayer.isConnected()) {
      this.sendLayer.send(msg);
      return (true);
    } else {
      return (false);
    }
  }

  public void receiveMessage(OOOProtocolMessage msg) {
    Assert.assertNotNull("Receive layer is null.", this.receiveLayer);
    Assert.assertNotNull("Attempt to null msg", msg);
    Assert.eval(msg.isSend());

    this.receiveLayer.receive(msg.getPayload());
  }

  public OOOProtocolMessage createProtocolMessage(long sequence, final TCNetworkMessage msg) {
    OOOProtocolMessage rv = messageFactory.createNewSendMessage(getSessionId(), sequence, msg);
    final Runnable callback = msg.getSentCallback();
    if (callback != null) {
      rv.setSentCallback(new Runnable() {
        public void run() {
          callback.run();
        }
      });
    }

    return rv;
  }

  private OOOProtocolMessage createProtocolMessage(TCByteBuffer[] msgData) {
    try {
      return messageParser.parseMessage(msgData);
    } catch (TCProtocolException e) {
      // XXX: this isn't the right thing to do here
      throw new TCRuntimeException(e);
    }
  }

  public void attachNewConnection(TCConnection connection) {
    throw new AssertionError("Must not call!");
  }

  public ConnectionID getConnectionId() {
    return sendLayer != null ? sendLayer.getConnectionId() : null;
  }

  public TCSocketAddress getLocalAddress() {
    return sendLayer.getLocalAddress();
  }

  public TCSocketAddress getRemoteAddress() {
    return sendLayer.getRemoteAddress();
  }

  public void receiveTransportMessage(WireProtocolMessage message) {
    throw new AssertionError("Must not call!");
  }

  public void sendToConnection(TCNetworkMessage message) {
    throw new AssertionError("Must not call!");
  }

  public void startRestoringConnection() {
    debugLog("Switched to restoreConnection mode");
    reconnectMode.set(true);
  }

  public void connectionRestoreFailed() {
    debugLog("RestoreConnectionFailed - resetting stack");
    resetStack();
    if (channelConnected.get()) {
      receiveLayer.notifyTransportDisconnected(this);
      channelConnected.set(false);
    }
  }

  private void resetStack() {
    // we need to reset because we are talking to a new stack on the other side
    reconnectMode.set(false);
    delivery.pause();
    delivery.reset();
  }

  public boolean isClosed() {
    return isClosed;
  }

  private short newRandomSessionId() {
    // generate a random session id
    Random r = new Random();
    r.setSeed(System.currentTimeMillis());
    return ((short) r.nextInt(Short.MAX_VALUE));
  }

}
