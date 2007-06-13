/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
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
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * NetworkLayer implementation for once and only once message delivery protocol.
 */
public class OnceAndOnlyOnceProtocolNetworkLayerImpl extends AbstractMessageTransport implements
    OnceAndOnlyOnceProtocolNetworkLayer, OOOProtocolMessageDelivery {
  public static final int                 MAX_SEND_QUEUE_SIZE  = 1000;
  private static final TCLogger           logger              = TCLogging
                                                                  .getLogger(OnceAndOnlyOnceProtocolNetworkLayerImpl.class);
  private final OOOProtocolMessageFactory messageFactory;
  private final OOOProtocolMessageParser  messageParser;
  boolean                                 wasConnected        = false;
  private MessageChannelInternal          receiveLayer;
  private MessageTransport                sendLayer;
  private GuaranteedDeliveryProtocol      delivery;
  private final SynchronizedBoolean       restoringConnection = new SynchronizedBoolean(false);
  private boolean                         isClosed            = false;

  public OnceAndOnlyOnceProtocolNetworkLayerImpl(OOOProtocolMessageFactory messageFactory,
                                                 OOOProtocolMessageParser messageParser, Sink workSink) {
    super(logger);
    this.messageFactory = messageFactory;
    this.messageParser = messageParser;
    // Use BoundedLinkedQueue to prevent outgrow of queue and causing OOME, refer DEV-710
    this.delivery = new GuaranteedDeliveryProtocol(this, workSink, new BoundedLinkedQueue(MAX_SEND_QUEUE_SIZE));
    this.delivery.start();
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
    if (msg.isGoodbye()) {
      isClosed = true;
      sendLayer.close();
      receiveLayer.close();
      delivery.pause();
      return;
    } else if (restoringConnection.get() && msg.isAck() && msg.getAckSequence() == -1) {
      resetStack();
      receiveLayer.notifyTransportDisconnected(this);
      this.notifyTransportConnected(this);
    } else {
      delivery.receive(msg);
    }
  }

  private void resetStack() {
    // we need to reset because we are talking to a new stack on the other side
    restoringConnection.set(false);
    delivery.pause();
    delivery.reset();
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
    OOOProtocolMessage opm = messageFactory.createNewGoodbyeMessage();
    opm.setSessionId(delivery.getSenderSessionId());
    sendLayer.send(opm);
    sendLayer.close();
  }

  /*********************************************************************************************************************
   * Transport listener interface...
   */

  public void notifyTransportConnected(MessageTransport transport) {
    logNotifyTransportConnected(transport);
    this.delivery.resume();
    if (!restoringConnection.get()) receiveLayer.notifyTransportConnected(this);
  }

  private void logNotifyTransportConnected(MessageTransport transport) {
    if (logger.isDebugEnabled()) {
      logger.debug("notifyTransportConnected(" + transport + ")");
    }
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    this.delivery.pause();
    if (!restoringConnection.get()) receiveLayer.notifyTransportDisconnected(this);
  }

  public void pause() {
    this.delivery.pause();
  }

  public void resume() {
    this.delivery.resume();
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    if (!restoringConnection.get()) receiveLayer.notifyTransportConnectAttempt(this);
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // XXX: do we do anything here? We've probably done everything we need to do when close() was called.
    receiveLayer.notifyTransportClosed(this);
  }

  /*********************************************************************************************************************
   * Protocol Message Delivery interface
   */

  public OOOProtocolMessage createAckRequestMessage() {
    return(this.messageFactory.createNewAckRequestMessage());
  }
  
  public void sendAckRequest() {
    sendToSendLayer(createAckRequestMessage());
  }
  
  public OOOProtocolMessage createAckMessage(long sequence) {
    return(this.messageFactory.createNewAckMessage(sequence));
  }
  
  public void sendAck(long sequence) {
    sendToSendLayer(createAckMessage(sequence));
  }

  public void sendMessage(OOOProtocolMessage msg) {
    sendToSendLayer(msg);
  }

  public void receiveMessage(OOOProtocolMessage msg) {
    Assert.assertNotNull("Receive layer is null.", this.receiveLayer);
    Assert.assertNotNull("Attempt to null msg", msg);
    Assert.eval(msg.isSend());

    this.receiveLayer.receive(msg.getPayload());
  }

  public OOOProtocolMessage createProtocolMessage(long sequence, final TCNetworkMessage msg) {
    OOOProtocolMessage rv = messageFactory.createNewSendMessage(sequence, msg);

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

  private void sendToSendLayer(OOOProtocolMessage msg) {
    // this method doesn't do anything at the moment, but it is a good spot to plug in things you might want to do
    // every message flowing down from the layer (like logging for example)
    this.sendLayer.send(msg);
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
    return sendLayer.getConnectionId();
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
    restoringConnection.set(true);
  }

  public void connectionRestoreFailed() {
    resetStack();
    receiveLayer.notifyTransportDisconnected(this);
  }

  public boolean isClosed() {
    return isClosed;
  }

}
