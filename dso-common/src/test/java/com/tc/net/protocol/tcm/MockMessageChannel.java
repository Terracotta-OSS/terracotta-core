/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.license.ProductID;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.MessageTransport;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MockMessageChannel implements MessageChannelInternal {

  private final ChannelID  channelId;
  private NetworkLayer     sendLayer;

  BlockingQueue<Object>    closedCalls = new LinkedBlockingQueue<Object>();
  private long             lastClosedCallTimestamp;

  private final Map        knownMessageTypes;

  private int              numSends;
  private TCNetworkMessage lastSentMessage;

  private NodeID           source      = ClientID.NULL_ID;
  private NodeID           destination = ServerID.NULL_ID;

  public MockMessageChannel(ChannelID channelId) {
    this.channelId = channelId;
    this.knownMessageTypes = new HashMap();
    reset();
    source = new ClientID(channelId.toLong());
  }

  public void registerType(TCMessageType messageType, Class theClass) {
    this.knownMessageTypes.put(messageType, theClass);
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
    throw new ImplementMe();
  }

  @Override
  public boolean isConnected() {
    throw new ImplementMe();
  }

  @Override
  public boolean isOpen() {
    throw new ImplementMe();
  }

  @Override
  public boolean isClosed() {
    throw new ImplementMe();
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

  public boolean waitForCloseCall(long timeout) throws InterruptedException {
    return closedCalls.poll(timeout, TimeUnit.MILLISECONDS) != null;
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
    throw new ImplementMe();
  }

  @Override
  public void send(TCNetworkMessage message) {
    ++this.numSends;
    this.lastSentMessage = message;
  }

  @Override
  public void receive(TCByteBuffer[] msgData) {
    throw new ImplementMe();
  }

  @Override
  public NetworkStackID open() {
    throw new ImplementMe();
  }

  @Override
  public NetworkStackID open(char[] password) {
    throw new ImplementMe();
  }

  public Class getRegisteredMessageClass(TCMessageType type) {
    return (Class) this.knownMessageTypes.get(type);
  }

  @Override
  public TCMessage createMessage(TCMessageType type) {
    Class theClass = (Class) this.knownMessageTypes.get(type);

    if (theClass == null) throw new ImplementMe();

    try {
      Constructor constructor = theClass.getConstructor(new Class[] { MessageMonitor.class, TCByteBufferOutput.class,
          MessageChannel.class, TCMessageType.class });
      return (TCMessage) constructor.newInstance(new Object[] { new NullMessageMonitor(),
          new TCByteBufferOutputStream(4, 4096, false), this, type });
    } catch (Exception e) {
      throw new ImplementMe("Failed", e);
    }
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    throw new ImplementMe();

  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    throw new ImplementMe();

  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    throw new ImplementMe();

  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    throw new ImplementMe();
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    throw new ImplementMe();
  }

  public long getLastClosedCallTimestamp() {
    return lastClosedCallTimestamp;
  }

  public NetworkLayer getSendLayer() {
    return sendLayer;
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

  @Override
  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  @Override
  public TCSocketAddress getRemoteAddress() {
    throw new ImplementMe();
  }

  @Override
  public short getStackLayerFlag() {
    throw new ImplementMe();
  }

  @Override
  public String getStackLayerName() {
    throw new ImplementMe();
  }

  @Override
  public NetworkLayer getReceiveLayer() {
    throw new ImplementMe();
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
  public ProductID getProductId() {
    return null;
  }
}
