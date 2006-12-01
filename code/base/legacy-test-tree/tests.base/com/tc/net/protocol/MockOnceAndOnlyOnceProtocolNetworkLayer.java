/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayer;
import com.tc.net.protocol.transport.MessageTransport;

/**
 * 
 */
public class MockOnceAndOnlyOnceProtocolNetworkLayer implements OnceAndOnlyOnceProtocolNetworkLayer {

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

}
