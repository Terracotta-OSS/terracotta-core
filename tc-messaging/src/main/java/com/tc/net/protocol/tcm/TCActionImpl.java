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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.util.AbstractIdentifier;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;

/**
 * @author teck
 */
public abstract class TCActionImpl implements TCAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCActionImpl.class);
  private final MessageMonitor          monitor;
  private final SetOnceFlag             processed         = new SetOnceFlag();
  private final SetOnceFlag             isSent            = new SetOnceFlag();
  private final TCMessageType           type;
  private final MessageChannel          channel;
  private final boolean                 isOutgoing;
  private int                           nvCount;
  private TCByteBufferOutputStream      out;
  private TCByteBufferInputStream       bbis;
  private int                           messageVersion;

  /**
   * Creates a new TCMessage to write data into (ie. to send to the network)
   */
  protected TCActionImpl(MessageMonitor monitor, TCByteBufferOutputStream output,
                          MessageChannel channel, TCMessageType type) {
    this.monitor = monitor;
    this.type = type;
    this.channel = channel;

    // this.bbos = new TCByteBufferOutputStream(4, 4096, false);
    this.out = output;

    // write out a zero. When dehydrated, this space will be replaced with the NV count
    this.out.writeInt(0);

    this.isOutgoing = true;
  }

  /**
   * Creates a new TCMessage object backed by the given data array (used when messages are read from the network)
   * 
   * @param header
   * @param data
   */
  protected TCActionImpl(MessageMonitor monitor, MessageChannel channel, TCMessageHeader header,
                          TCByteBufferInputStream data) {
    this.monitor = monitor;
    this.type = TCMessageType.getInstance(header.getMessageType());
    this.messageVersion = header.getMessageTypeVersion();
    this.bbis = data;
    this.channel = channel;
    this.isOutgoing = false;
  }

  @Override
  public TCMessageType getMessageType() {
    return type;
  }

  protected int getMessageVersion() {
    return this.messageVersion;
  }

  protected void setMessageVersion(int version) {
    this.messageVersion = version;
  }

  // use me to read directly from the message data (as opposed to using the name-value mechanism)
  protected TCByteBufferInputStream getInputStream() {
    return this.bbis;
  }

  // use me to write directly to the message data (as opposed to using the name-value mechanism)
  protected TCByteBufferOutputStream getOutputStream() {
    return this.out;
  }

  protected abstract void dehydrateValues();

  protected TCActionNetworkMessage convertToNetworkMessage() {
    TCActionNetworkMessage msg = new TCActionNetworkMessageImpl(new TCMessageHeaderImpl(type), ()->getDataBuffers());
    if (this.isOutgoing) {
      msg.addCompleteCallback(out::reset);
    }
    return msg;
  }

  public TCByteBuffer[] getDataBuffers() {
    dehydrateValues();

    final TCByteBuffer[] nvData = out.toArray();

    Assert.eval(nvData.length > 0);
    nvData[0].putInt(0, nvCount);
    return nvData;
  }

  /**
   * Reads the payload byte buffer data and sets instance data. This should be called after the message is read from the
   * network before it is released to the client for use. XXX:: This synchronization is there to create proper memory
   * boundary.
   */
  @Override
  public synchronized void hydrate() throws IOException, UnknownNameException {
    if (processed.attemptSet()) {
      try {
        final int count = bbis.readInt();
        if (count < 0) { throw new IOException("negative NV count: " + count); }

        for (int i = 0; i < count; i++) {
          final byte name = bbis.readByte();
          if (!hydrateValue(name)) {
            LOGGER.error(" Hydrate Error - " + toString());
            throw new UnknownNameException(getClass(), name);
          }
        }
      } finally {
        this.bbis.reset();
        this.bbis = null;
      }
      monitor.newIncomingMessage(this);
    }
  }

  /**
   * Subclasses must implement this to set appropriate instance variables with the value of the given name.
   * Return false if the given name is unknown to your message class
   * 
   * @param name
   */
  protected abstract boolean hydrateValue(byte name) throws IOException;

  protected boolean getBooleanValue() throws IOException {
    return bbis.readBoolean();
  }

  protected byte getByteValue() throws IOException {
    return bbis.readByte();
  }

  protected char getCharValue() throws IOException {
    return bbis.readChar();
  }

  protected double getDoubleValue() throws IOException {
    return bbis.readDouble();
  }

  protected float getFloatValue() throws IOException {
    return bbis.readFloat();
  }

  protected int getIntValue() throws IOException {
    return bbis.readInt();
  }

  protected long getLongValue() throws IOException {
    return bbis.readLong();
  }

  protected short getShortValue() throws IOException {
    return bbis.readShort();
  }

  protected NodeID getNodeIDValue() throws IOException {
    return getObject(new NodeIDSerializer()).getNodeID();
  }

  protected <T extends TCSerializable<T>> T getObject(T target) throws IOException {
    return target.deserializeFrom(bbis);
  }

  protected String getStringValue() throws IOException {
    return bbis.readString();
  }

  protected byte[] getBytesArray() throws IOException {
    int length = bbis.readInt();    
    byte bytes[] = new byte[length];
    bbis.readFully(bytes);
    return bytes;
  }

  protected TCByteBuffer getByteBuffer() throws IOException {
    int length = bbis.readInt();
    return bbis.read(length);
  }
  
  protected void putNVPair(byte name, boolean value) {
    nvCount++;
    out.write(name);
    out.writeBoolean(value);
  }

  protected void putNVPair(byte name, byte value) {
    nvCount++;
    out.write(name);
    out.writeByte(value);
  }

  protected void putNVPair(byte name, char value) {
    nvCount++;
    out.write(name);
    out.writeChar(value);
  }

  protected void putNVPair(byte name, double value) {
    nvCount++;
    out.write(name);
    out.writeDouble(value);
  }

  protected void putNVPair(byte name, float value) {
    nvCount++;
    out.write(name);
    out.writeFloat(value);
  }

  protected void putNVPair(byte name, int value) {
    nvCount++;
    out.write(name);
    out.writeInt(value);
  }

  protected void putNVPair(byte name, long value) {
    nvCount++;
    out.write(name);
    out.writeLong(value);
  }

  protected void putNVPair(byte name, short value) {
    nvCount++;
    out.write(name);
    out.writeShort(value);
  }

  protected void putNVPair(byte name, String value) {
    nvCount++;
    out.write(name);
    out.writeString(value);
  }

  protected void putNVPair(byte name, NodeID nodeID) {
    nvCount++;
    out.write(name);
    new NodeIDSerializer(nodeID).serializeTo(out);
  }

  protected void putNVPair(byte name, TCSerializable<?> object) {
    nvCount++;
    out.write(name);
    object.serializeTo(out);
  }

  protected void putNVPair(byte name, TCByteBuffer[] data) {
    nvCount++;
    out.write(name);
    out.write(data);
  }

  protected void putNVPair(byte name, byte[] bytes) {
    nvCount++;
    out.write(name);
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  protected void putNVPair(byte name, AbstractIdentifier identifier) {
    nvCount++;
    out.write(name);
    out.writeLong(identifier.toLong());
  }

  @Override
  public MessageChannel getChannel() {
    return channel;
  }

  /*
   * (non-Javadoc)
   * @see com.tc.net.protocol.tcm.ApplicationMessage#send()
   */
  @Override
  public NetworkRecall send() {
    if (isSent.attemptSet()) {
      TCActionNetworkMessage msg = convertToNetworkMessage();
      try {
        basicSend(msg);
        return msg::cancel;
      } catch (IOException ioe) {
//  suppress some warnings when the channel is closed as this is expected, client is not
//  there anymore
        if (channel.isOpen()) {
          LOGGER.info("Message not sent: " + ioe.getMessage());
        }
      }
    }
    return null;
  }

  private void basicSend(TCActionNetworkMessage msg) throws IOException {
    channel.send(msg);
    monitor.newOutgoingMessage(this);
  }

  @Override
  public NodeID getSourceNodeID() {
    return isOutgoing ? channel.getLocalNodeID() : channel.getRemoteNodeID();
  }

  @Override
  public NodeID getDestinationNodeID() {
    return isOutgoing ? channel.getRemoteNodeID() : channel.getLocalNodeID();
  }

  @Override
  public int getMessageLength() {
    return (TCMessageHeader.MIN_LENGTH / 4) + (isOutgoing ? out.getBytesWritten() : bbis.getTotalLength());
  }
}

