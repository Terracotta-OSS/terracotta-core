/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.license.ProductID;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.List;

public class TestMessageChannel implements MessageChannel {

  public List                   createMessageContexts = new ArrayList();
  public NoExceptionLinkedQueue sendQueue             = new NoExceptionLinkedQueue();
  public TCMessage              message;
  public ChannelID              channelID             = new ChannelID(1);
  private NodeID                source                = ClientID.NULL_ID;
  private NodeID                destination           = ServerID.NULL_ID;

  @Override
  public void addListener(ChannelEventListener listener) {
    return;
  }

  @Override
  public NodeID getLocalNodeID() {
    if (source == ClientID.NULL_ID) {
      source = new ClientID(getChannelID().toLong());
    }
    return source;
  }

  @Override
  public void setLocalNodeID(NodeID source) {
    this.source = source;
  }

  @Override
  public NodeID getRemoteNodeID() {
    return destination;
  }

  public void setRemoteNodeID(NodeID destination) {
    this.destination = destination;
  }

  @Override
  public boolean isConnected() {
    return false;
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public boolean isClosed() {
    return false;
  }

  @Override
  public TCMessage createMessage(TCMessageType type) {
    createMessageContexts.add(new CreateMessageContext(type, this.message));
    return this.message;
  }

  @Override
  public void close() {
    return;
  }

  @Override
  public ChannelID getChannelID() {
    return channelID;
  }

  public void setSendLayer(NetworkLayer layer) {
    return;
  }

  public void setReceiveLayer(NetworkLayer layer) {
    return;
  }

  @Override
  public void send(TCNetworkMessage msg) {
    sendQueue.put(msg);
  }

  public void receive(TCByteBuffer[] msgData) {
    return;
  }

  @Override
  public NetworkStackID open() {
    return null;
  }

  @Override
  public NetworkStackID open(char[] password) {
    return null;
  }

  public void notifyTransportConnected(MessageTransport transport) {
    return;
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    return;
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    return;
  }

  public void notifyTransportClosed(MessageTransport transport) {
    return;
  }

  @Override
  public Object getAttachment(String key) {
    throw new ImplementMe();
  }

  @Override
  public void addAttachment(String key, Object value, boolean replace) {
    throw new ImplementMe();
  }

  @Override
  public Object removeAttachment(String key) {
    throw new ImplementMe();
  }

  public String getRemoteSocketInfo() {
    throw new ImplementMe();
  }

  @Override
  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  @Override
  public TCSocketAddress getRemoteAddress() {
    throw new ImplementMe();
  }

  public static class CreateMessageContext {
    public final TCMessageType type;
    public final TCMessage     returnedMessage;

    private CreateMessageContext(TCMessageType type, TCMessage returnedMessage) {
      this.type = type;
      this.returnedMessage = returnedMessage;
    }
  }

  @Override
  public ProductID getProductId() {
    return null;
  }
}
