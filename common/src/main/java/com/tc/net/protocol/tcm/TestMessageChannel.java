/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.util.ProductID;
import com.tc.net.ClientID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import java.io.IOException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestMessageChannel implements MessageChannel {

  public List<CreateMessageContext>               createMessageContexts = new ArrayList<CreateMessageContext>();
  public NoExceptionLinkedQueue<TCNetworkMessage> sendQueue             = new NoExceptionLinkedQueue<TCNetworkMessage>();
  public TCMessage                                message;
  public ChannelID                                channelID             = new ChannelID(1);
  private NodeID                                  source                = ClientID.NULL_ID;
  private NodeID                                  destination           = ServerID.NULL_ID;

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
  public NetworkStackID open(ConnectionInfo info) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException {
    return null;
  }

  @Override
  public NetworkStackID open(Collection<ConnectionInfo> info) {
    return null;
  }

  @Override
  public NetworkStackID open(Collection<ConnectionInfo> info, String username, char[] password) {
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
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAttachment(String key, Object value, boolean replace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object removeAttachment(String key) {
    throw new UnsupportedOperationException();
  }

  public String getRemoteSocketInfo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCSocketAddress getLocalAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCSocketAddress getRemoteAddress() {
    throw new UnsupportedOperationException();
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
