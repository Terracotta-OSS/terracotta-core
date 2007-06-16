/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.logging.NullTCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayer;
import com.tc.net.protocol.transport.AbstractMessageTransport;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.WireProtocolMessage;

/**
 * 
 */
public class MockOnceAndOnlyOnceProtocolNetworkLayer extends AbstractMessageTransport implements
    OnceAndOnlyOnceProtocolNetworkLayer {

  public MockOnceAndOnlyOnceProtocolNetworkLayer() {
    super(new NullTCLogger());
  }

  public NetworkLayer sendLayer;
  public NetworkLayer receiveLayer;

  public void setSendLayer(NetworkLayer layer) {
    this.sendLayer = layer;
  }

  public void setReceiveLayer(NetworkLayer layer) {
    this.receiveLayer = layer;
  }

  public void send(TCNetworkMessage message) {
    throw new ImplementMe();
  }

  public void receive(TCByteBuffer[] msgData) {
    throw new ImplementMe();
  }

  public boolean isConnected() {
    throw new ImplementMe();
  }

  public NetworkStackID open() {
    throw new ImplementMe();
  }

  public void close() {
    throw new ImplementMe();
  }

  public void notifyTransportConnected(MessageTransport transport) {
    throw new ImplementMe();
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    throw new ImplementMe();
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    throw new ImplementMe();
  }

  public void notifyTransportClosed(MessageTransport transport) {
    throw new ImplementMe();
  }

  public void attachNewConnection(TCConnection connection) {
    throw new ImplementMe();

  }

  public ConnectionID getConnectionId() {
    throw new ImplementMe();
  }

  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  public TCSocketAddress getRemoteAddress() {
    throw new ImplementMe();
  }

  public void receiveTransportMessage(WireProtocolMessage message) {
    throw new ImplementMe();

  }

  public void sendToConnection(TCNetworkMessage message) {
    throw new ImplementMe();

  }
  
  public void start() {
    throw new ImplementMe();
  }

  public void pause() {
    throw new ImplementMe();
  }

  public void resume() {
    throw new ImplementMe();

  }

  public void connectionRestoreFailed() {
    throw new ImplementMe();

  }

  public void startRestoringConnection() {
    throw new ImplementMe();

  }

  public boolean isClosed() {
    throw new ImplementMe();
  }
}
