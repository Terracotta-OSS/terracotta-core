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
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
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
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.util.TCTimeoutException;
import java.io.IOException;

import java.lang.reflect.Constructor;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MockMessageChannel implements MessageChannelInternal {

  private final ChannelID  channelId;
  private NetworkLayer     sendLayer;

  BlockingQueue<Object>    closedCalls = new LinkedBlockingQueue<Object>();
  private long             lastClosedCallTimestamp;

  private final Map<TCMessageType, Class<? extends TCMessage>>        knownMessageTypes;

  private int              numSends;
  private TCNetworkMessage lastSentMessage;

  private NodeID           source      = ClientID.NULL_ID;
  private final NodeID           destination = ServerID.NULL_ID;

  public MockMessageChannel(ChannelID channelId) {
    this.channelId = channelId;
    this.knownMessageTypes = new HashMap<TCMessageType, Class<? extends TCMessage>>();
    reset();
    source = new ClientID(channelId.toLong());
  }

  @Override
  public void reset() {
    this.numSends = 0;
    this.lastSentMessage = null;
  }

  public TCNetworkMessage getLastSentMessage() {
    return lastSentMessage;
  }

  public int getNumSends() {
    return numSends;
  }

  @Override
  public void addListener(ChannelEventListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isConnected() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isOpen() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isClosed() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    this.lastClosedCallTimestamp = System.currentTimeMillis();
    try {
      closedCalls.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public ChannelID getChannelID() {
    return channelId;
  }

  @Override
  public void setSendLayer(NetworkLayer layer) {
    this.sendLayer = layer;
  }

  @Override
  public void setReceiveLayer(NetworkLayer layer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void send(TCNetworkMessage message) {
    ++this.numSends;
    this.lastSentMessage = message;
  }

  @Override
  public void receive(TCByteBuffer[] msgData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkStackID open(ConnectionInfo info) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkStackID open(Collection<ConnectionInfo> info) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkStackID open(Collection<ConnectionInfo> info, String username, char[] password) throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException, CommStackMismatchException {
    throw new UnsupportedOperationException();
  }
  
  @SuppressWarnings("resource")
  @Override
  public TCMessage createMessage(TCMessageType type) {
    Class<? extends TCMessage> theClass = this.knownMessageTypes.get(type);

    if (theClass == null) throw new UnsupportedOperationException();

    try {
      Constructor<? extends TCMessage> constructor = theClass.getConstructor(new Class[] { MessageMonitor.class, TCByteBufferOutput.class,
          MessageChannel.class, TCMessageType.class });
      return constructor.newInstance(new Object[] { new NullMessageMonitor(),
          new TCByteBufferOutputStream(4, 4096, false), this, type });
    } catch (Exception e) {
      throw new UnsupportedOperationException("Failed", e);
    }
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    throw new UnsupportedOperationException();
  }

  public long getLastClosedCallTimestamp() {
    return lastClosedCallTimestamp;
  }

  public NetworkLayer getSendLayer() {
    return sendLayer;
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

  @Override
  public TCSocketAddress getLocalAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCSocketAddress getRemoteAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public short getStackLayerFlag() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getStackLayerName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkLayer getReceiveLayer() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NodeID getLocalNodeID() {
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

  @Override
  public ProductID getProductID() {
    return null;
  }

  @Override
  public ConnectionID getConnectionID() {
    return null;
  }
}
