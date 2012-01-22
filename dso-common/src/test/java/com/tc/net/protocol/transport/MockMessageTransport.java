/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.List;

public class MockMessageTransport implements MessageTransport {
  public ConnectionID connectionId;
  public NetworkLayer receiveLayer;

  public List         listeners = new ArrayList();

  public ConnectionID getConnectionId() {
    return this.connectionId;
  }

  public void addTransportListeners(List toAdd) {
    listeners.addAll(toAdd);
  }

  public void addTransportListener(MessageTransportListener listener) {
    listeners.add(listener);
  }

  public void removeTransportListeners() {
    listeners.clear();
  }

  public void setSendLayer(NetworkLayer layer) {
    throw new ImplementMe();
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

  public void attachNewConnection(TCConnection connection) {
    throw new ImplementMe();
  }

  public void receiveTransportMessage(WireProtocolMessage message) {
    throw new ImplementMe();
  }

  public final NoExceptionLinkedQueue sendToConnectionCalls = new NoExceptionLinkedQueue();

  public void sendToConnection(TCNetworkMessage message) {
    sendToConnectionCalls.put(message);
  }

  public TCSocketAddress getRemoteAddress() {
    throw new ImplementMe();
  }

  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  public void setAllowConnectionReplace(boolean b) {
    throw new ImplementMe();
  }

  public short getStackLayerFlag() {
    throw new ImplementMe();
  }

  public String getStackLayerName() {
    throw new ImplementMe();
  }

  public NetworkLayer getReceiveLayer() {
    throw new ImplementMe();
  }

  public short getCommunicationStackFlags(NetworkLayer parentLayer) {
    return NetworkLayer.TYPE_TEST_MESSAGE;
  }

  public String getCommunicationStackNames(NetworkLayer parentLayer) {
    throw new ImplementMe();
  }

  public void setRemoteCallbackPort(int callbackPort) {
    //
  }

  public int getRemoteCallbackPort() {
    throw new ImplementMe();
  }

  public void initConnectionID(ConnectionID cid) {
    connectionId = cid;
  }
}
