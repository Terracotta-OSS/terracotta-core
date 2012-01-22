/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
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

public class MockMessageChannel implements MessageChannelInternal {

  private final ChannelID  channelId;
  private NetworkLayer     sendLayer;

  LinkedQueue              closedCalls = new LinkedQueue();
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

  public void addListener(ChannelEventListener listener) {
    throw new ImplementMe();
  }

  public boolean isConnected() {
    throw new ImplementMe();
  }

  public boolean isOpen() {
    throw new ImplementMe();
  }

  public boolean isClosed() {
    throw new ImplementMe();
  }

  public void close() {
    this.lastClosedCallTimestamp = System.currentTimeMillis();
    try {
      closedCalls.put(new Object());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public boolean waitForCloseCall(long timeout) throws InterruptedException {
    return closedCalls.poll(timeout) != null;
  }

  public ChannelID getChannelID() {
    return channelId;
  }

  public void setSendLayer(NetworkLayer layer) {
    this.sendLayer = layer;
  }

  public void setReceiveLayer(NetworkLayer layer) {
    throw new ImplementMe();
  }

  public void send(TCNetworkMessage message) {
    ++this.numSends;
    this.lastSentMessage = message;
  }

  public void receive(TCByteBuffer[] msgData) {
    throw new ImplementMe();
  }

  public NetworkStackID open() {
    throw new ImplementMe();
  }

  public Class getRegisteredMessageClass(TCMessageType type) {
    return (Class) this.knownMessageTypes.get(type);
  }

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

  public void notifyTransportConnected(MessageTransport transport) {
    throw new ImplementMe();

  }

  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    throw new ImplementMe();

  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    throw new ImplementMe();

  }

  public void notifyTransportClosed(MessageTransport transport) {
    throw new ImplementMe();
  }

  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    throw new ImplementMe();
  }

  public long getLastClosedCallTimestamp() {
    return lastClosedCallTimestamp;
  }

  public NetworkLayer getSendLayer() {
    return sendLayer;
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

  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  public TCSocketAddress getRemoteAddress() {
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

  public NodeID getLocalNodeID() {
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

}
