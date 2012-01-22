/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
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

  public void addListener(ChannelEventListener listener) {
    return;
  }

  public NodeID getLocalNodeID() {
    if (source == ClientID.NULL_ID) {
      source = new ClientID(getChannelID().toLong());
    }
    return source;
  }

  public void setLocalNodeID(NodeID source) {
    this.source = source;
  }

  public NodeID getRemoteNodeID() {
    return destination;
  }

  public void setRemoteNodeID(NodeID destination) {
    this.destination = destination;
  }

  public boolean isConnected() {
    return false;
  }

  public boolean isOpen() {
    return false;
  }

  public boolean isClosed() {
    return false;
  }

  public TCMessage createMessage(TCMessageType type) {
    createMessageContexts.add(new CreateMessageContext(type, this.message));
    return this.message;
  }

  public void close() {
    return;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public void setSendLayer(NetworkLayer layer) {
    return;
  }

  public void setReceiveLayer(NetworkLayer layer) {
    return;
  }

  public void send(TCNetworkMessage msg) {
    sendQueue.put(msg);
  }

  public void receive(TCByteBuffer[] msgData) {
    return;
  }

  public NetworkStackID open() {
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

  public Object getAttachment(String key) {
    throw new ImplementMe();
  }

  public void addAttachment(String key, Object value, boolean replace) {
    throw new ImplementMe();
  }

  public Object removeAttachment(String key) {
    throw new ImplementMe();
  }

  public String getRemoteSocketInfo() {
    throw new ImplementMe();
  }

  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

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
}
