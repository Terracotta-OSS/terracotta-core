/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogging;
import com.tc.net.CommStackMismatchException;
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
import com.tc.properties.ReconnectConfig;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NetworkLayer implementation for once and only once message delivery protocol.
 */
public class OnceAndOnlyOnceProtocolNetworkLayerImpl extends AbstractMessageTransport implements
    OnceAndOnlyOnceProtocolNetworkLayer, OOOProtocolMessageDelivery {

  private final OOOProtocolMessageFactory  messageFactory;
  private final OOOProtocolMessageParser   messageParser;
  boolean                                  wasConnected     = false;
  private MessageChannelInternal           receiveLayer;
  private MessageTransport                 sendLayer;
  private final GuaranteedDeliveryProtocol delivery;
  private final AtomicBoolean              reconnectMode    = new AtomicBoolean(false);
  private final AtomicBoolean              handshakeMode    = new AtomicBoolean(false);
  private final AtomicBoolean              channelConnected = new AtomicBoolean(false);
  private boolean                          isClosed         = false;
  private final boolean                    isClient;
  private final String                     debugId;
  private UUID                             sessionId        = UUID.NULL_ID;
  private final Timer                      restoreConnectTimer;
  private static final boolean             debug            = false;

  public OnceAndOnlyOnceProtocolNetworkLayerImpl(OOOProtocolMessageFactory messageFactory,
                                                 OOOProtocolMessageParser messageParser,
                                                 ReconnectConfig reconnectConfig, boolean isClient) {
    this(messageFactory, messageParser, reconnectConfig, isClient, null);
  }

  public OnceAndOnlyOnceProtocolNetworkLayerImpl(OOOProtocolMessageFactory messageFactory,
                                                 OOOProtocolMessageParser messageParser,
                                                 ReconnectConfig reconnectConfig, boolean isClient,
                                                 Timer restoreConnectTimer) {
    super(TCLogging.getLogger(OnceAndOnlyOnceProtocolNetworkLayerImpl.class));
    this.messageFactory = messageFactory;
    this.messageParser = messageParser;
    this.isClient = isClient;
    this.delivery = new GuaranteedDeliveryProtocol(this, reconnectConfig, isClient);
    this.delivery.start();
    this.delivery.pause();
    this.restoreConnectTimer = restoreConnectTimer;
    this.sessionId = (this.isClient) ? UUID.NULL_ID : UUID.getUUID();
    this.debugId = (this.isClient) ? "CLIENT" : "SERVER";
  }

  /*********************************************************************************************************************
   * Network layer interface...
   */

  public void setNewSessionID() {
    this.sessionId = UUID.getUUID();
  }

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

  public NetworkLayer getReceiveLayer() {
    return this.receiveLayer;
  }

  public void send(TCNetworkMessage message) {
    delivery.send(message);
  }

  public void receive(TCByteBuffer[] msgData) {
    OOOProtocolMessage msg = createProtocolMessage(msgData);
    if (debug) {
      debugLog("receive -> " + msg.getHeader().toString());
    }

    if (msg.isSend() || msg.isAck()) {

      if (!sessionId.equals(msg.getSessionId())) {
        logger.warn("Dropping old session message " + msg);
        return;
      }

      if (handshakeMode.get()) {
        Assert.fail("Unexpected message while in handshaking mode: " + msg);
      }

      if (!channelConnected.get()) {
        logger.warn("Drop stale message " + msg.getHeader().toString() + " from " + sendLayer.getConnectionId());
        return;
      }

      delivery.receive(msg);
    } else if (msg.isHandshake()) {
      Assert.inv(!isClient);
      if (debug) {
        debugLog("Got Handshake message...");
      }

      if (msg.getSessionId().equals(UUID.NULL_ID)) {
        if (debug) {
          debugLog("A brand new client is trying to connect - reply OK");
        }
        OOOProtocolMessage reply = createHandshakeReplyOkMessage(delivery.getReceiver().getReceived());
        sendMessage(reply);

        delivery.resume();
        delivery.receive(createHandshakeReplyOkMessage(-1));

        resetModesAndfireTransportConnectedEvent();

      } else if (msg.getSessionId().equals(getSessionId())) {
        if (debug) {
          debugLog("A same-session client is trying to connect - reply OK");
        }
        OOOProtocolMessage reply = createHandshakeReplyOkMessage(delivery.getReceiver().getReceived());
        sendMessage(reply);

        delivery.resume();
        delivery.receive(createHandshakeReplyOkMessage(msg.getAckSequence()));

        resetModesAndfireTransportConnectedEvent();

      } else {
        if (debug) {
          debugLog("A DIFF-session client is trying to connect - request OOO Reset");
        }
        logger.info("Requesting OOO reset for different session client " + getConnectionId());

        long localAck = delivery.getReceiver().getReceived();
        sendMessage(createHandshakeReplyFailMessage(localAck));

        if (channelConnected.get()) {
          /*
           * Client has got some trouble in talking to me before and has resetted its OOO Stack. I am not going to
           * accept him again.
           */
          receiveLayer.notifyTransportDisconnected(this, false);
          channelConnected.set(false);
        } else {
          /*
           * Probably I am a newly starting up server and don't have this client priorly connected, will accept this
           * client with its resetted OOO Stack.
           */
        }

        // we need a new OOO stack
        resetStack();

        delivery.resume();
        delivery.receive(createHandshakeReplyOkMessage(-1));

        resetModesAndfireTransportConnectedEvent();

      }

    } else if (msg.isHandshakeReplyOk()) {
      Assert.inv(isClient);
      Assert.inv(handshakeMode.get());
      debugLog("Got reply OK");

      // current session is still ok:
      // 1. might have to resend some messages
      // 2. no need to signal to Higher Level
      sessionId = msg.getSessionId();
      delivery.resume();
      delivery.receive(msg);

      resetModesAndfireTransportConnectedEvent();

    } else if (msg.isHandshakeReplyFail()) {
      if (debug) {
        debugLog("Received handshake fail reply - request for OOO reset");
      }
      Assert.inv(isClient);
      Assert.inv(handshakeMode.get());

      // we need a new OOO stack and make a note of new sessionID
      resetStack();
      sessionId = msg.getSessionId();

      delivery.resume();
      delivery.receive(createHandshakeReplyOkMessage(-1));

      if (channelConnected.get()) {
        receiveLayer.notifyTransportDisconnected(this, false);
        channelConnected.set(false);
      }

      resetModesAndfireTransportConnectedEvent();

    } else if (msg.isGoodbye()) {
      if (debug) {
        debugLog("Got GoodBye message - shutting down");
      }

      if (isConnected()) {
        isClosed = true;
        sendLayer.close();
        receiveLayer.close();
        delivery.pause();
      } else {
        logger.warn("Channel not yet connected. Ignoring OOO Goodbye Message: ChannelConnected: "
                    + channelConnected.get() + "; DeliveryEngine: " + delivery);
      }
    } else {
      Assert.inv(false);
    }
  }

  private void resetModesAndfireTransportConnectedEvent() {
    handshakeMode.set(false);
    if (!channelConnected.get()) {
      channelConnected.set(true);
      receiveLayer.notifyTransportConnected(this);
    } else {
      DebugUtil.trace("OOOLayer-" + debugId + "-" + sendLayer.getConnectionId()
                      + " -> not firing Tx connected event to channel");
    }
    reconnectMode.set(false);
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("OOOLayer-" + debugId + "-" + sendLayer.getConnectionId() + " -> " + msg);
    }
  }

  public boolean isConnected() {
    return (channelConnected.get() && !delivery.isPaused());
  }

  public NetworkStackID open() throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Assert.assertNotNull(sendLayer);
    return sendLayer.open();
  }

  public void close() {
    Assert.assertNotNull(sendLayer);
    if (isClient) {
      // send goobye message with session-id on it in case of client. Server never sends goodbye message.
      OOOProtocolMessage opm = messageFactory.createNewGoodbyeMessage(getSessionId());
      sendMessage(opm);
    }
    sendLayer.close();
  }

  @Override
  public void initConnectionID(ConnectionID cid) {
    Assert.assertNotNull(sendLayer);
    sendLayer.initConnectionID(cid);
  }

  /*********************************************************************************************************************
   * Transport listener interface...
   */

  public void notifyTransportConnected(MessageTransport transport) {
    handshakeMode.set(true);
    if (isClient) {
      OOOProtocolMessage handshake = createHandshakeMessage(delivery.getReceiver().getReceived());
      debugLog("Sending Handshake message...");
      sendMessage(handshake);
    } else {
      // reuse for missing transportDisconnected events
      if (!delivery.isPaused()) {
        notifyTransportDisconnected(null, false);
      }
    }
    reconnectMode.set(false);
  }

  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    final boolean restoreConnectionMode = reconnectMode.get();
    debugLog("Transport Disconnected - pausing delivery, restoreConnection = " + restoreConnectionMode);
    this.delivery.pause();
    if (!restoreConnectionMode) {
      if (channelConnected.get()) receiveLayer.notifyTransportDisconnected(this, forcedDisconnect);
      channelConnected.set(false);
    }
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

  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    receiveLayer.notifyTransportReconnectionRejected(this);
  }

  /*********************************************************************************************************************
   * Protocol Message Delivery interface
   */

  public OOOProtocolMessage createHandshakeMessage(long ack) {
    OOOProtocolMessage rv = this.messageFactory.createNewHandshakeMessage(getSessionId(), ack);
    return rv;
  }

  public OOOProtocolMessage createHandshakeReplyOkMessage(long ack) {
    OOOProtocolMessage rv = this.messageFactory.createNewHandshakeReplyOkMessage(getSessionId(), ack);
    return rv;
  }

  public OOOProtocolMessage createHandshakeReplyFailMessage(long ack) {
    OOOProtocolMessage rv = this.messageFactory.createNewHandshakeReplyFailMessage(getSessionId(), ack);
    return rv;
  }

  private UUID getSessionId() {
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

  public void setAllowConnectionReplace(boolean allow) {
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

  public Timer getRestoreConnectTimer() {
    Assert.assertNotNull(this.restoreConnectTimer);
    return this.restoreConnectTimer;
  }

  public void connectionRestoreFailed() {
    debugLog("RestoreConnectionFailed - resetting stack");
    if (channelConnected.get()) {
      // forcedDisconnect flag is not useful in above layers. defaulting to false
      receiveLayer.notifyTransportDisconnected(this, false);
      channelConnected.set(false);
    }
    reconnectMode.set(false);
    delivery.pause();
    delivery.reset();
    sessionId = UUID.getUUID();
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

  /**
   * this function gets the stackLayerFlag, added to build the communication stack information
   */
  public short getStackLayerFlag() {
    // this is the OOO layer
    return TYPE_OOO_LAYER;
  }

  /**
   * This function gets the stack layer name of the present layer, added to build the communication stack information
   */
  public String getStackLayerName() {
    // this is the OOO layer
    return NAME_OOO_LAYER;
  }

  public void setRemoteCallbackPort(int callbackPort) {
    throw new AssertionError();
  }

  public int getRemoteCallbackPort() {
    throw new AssertionError();
  }

  // for testing
  public NetworkLayer getSendLayer() {
    return this.sendLayer;
  }
}
