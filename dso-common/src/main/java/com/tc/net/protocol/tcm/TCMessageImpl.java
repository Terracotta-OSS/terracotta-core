/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.net.protocol.AbstractTCNetworkMessage;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIDSerializer;
import com.tc.object.metadata.NVPair;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;

/**
 * @author teck
 */
public abstract class TCMessageImpl extends AbstractTCNetworkMessage implements TCMessage {

  private final MessageMonitor     monitor;
  private final SetOnceFlag        processed = new SetOnceFlag();
  private final SetOnceFlag        isSent    = new SetOnceFlag();
  private final TCMessageType      type;
  private final MessageChannel     channel;
  private final boolean            isOutgoing;
  private int                      nvCount;
  private TCByteBufferOutputStream out;
  private TCByteBufferInputStream  bbis;
  private int                      messageVersion;

  /**
   * Creates a new TCMessage to write data into (ie. to send to the network)
   */
  protected TCMessageImpl(final MessageMonitor monitor, final TCByteBufferOutputStream output,
                          final MessageChannel channel, final TCMessageType type) {
    super(new TCMessageHeaderImpl(type), false);
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
  protected TCMessageImpl(final MessageMonitor monitor, final MessageChannel channel, final TCMessageHeader header,
                          final TCByteBuffer[] data) {
    super(header, data);
    this.monitor = monitor;
    this.type = TCMessageType.getInstance(header.getMessageType());
    this.messageVersion = header.getMessageTypeVersion();
    this.bbis = new TCByteBufferInputStream(data);
    this.channel = channel;
    this.isOutgoing = false;
  }

  public TCMessageType getMessageType() {
    return type;
  }

  protected int getMessageVersion() {
    return this.messageVersion;
  }

  protected void setMessageVersion(final int version) {
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

  // use me to write directly to the message data (as opposed to using the name-value mechanism)
  // protected TCByteBufferOutputStream getOutputStream() {
  // return this.bbos;
  // }

  protected void dehydrateValues() {
    // override me to add NV data to your message
  }

  /**
   * Prepares all instance data into the payload byte buffer array in preparation for sending it.
   */
  public void dehydrate() {
    dehydrate(null);
  }

  private void dehydrate(TCByteBuffer[] nvData) {
    if (processed.attemptSet()) {
      try {
        if (nvData == null) nvData = nvToTCByteBufferArray();
        setPayload(nvData);
        populateHeader();
        seal();
      } catch (Throwable t) {
        t.printStackTrace();
        throw new RuntimeException(t);
      } finally {
        this.out.close();
        if (!isOutputStreamRecycled()) this.out = null;
      }
    }
  }

  private final TCByteBuffer[] nvToTCByteBufferArray() {
    dehydrateValues();

    final TCByteBuffer[] nvData = out.toArray();

    Assert.eval(nvData.length > 0);
    nvData[0].putInt(0, nvCount);
    return nvData;
  }

  private void populateHeader() {
    TCMessageHeader hdr = (TCMessageHeader) getHeader();
    hdr.setMessageType(getMessageType().getType());
    hdr.setMessageTypeVersion(getMessageVersion());
  }

  /**
   * Reads the payload byte buffer data and sets instance data. This should be called after the message is read from the
   * network before it is released to the client for use. XXX:: This synchronization is there to create proper memory
   * boundary.
   */
  public synchronized void hydrate() throws IOException, UnknownNameException {
    if (processed.attemptSet()) {
      try {
        final int count = bbis.readInt();
        if (count < 0) { throw new IOException("negative NV count: " + count); }

        for (int i = 0; i < count; i++) {
          final byte name = bbis.readByte();
          if (!hydrateValue(name)) {
            logger.error(" Hydrate Error - " + toString());
            throw new UnknownNameException(getClass(), name);
          }
        }
      } finally {
        this.bbis.close();
        this.bbis = null;
        doRecycleOnRead();
      }
      monitor.newIncomingMessage(this);
    }
  }

  // Can be overloaded by sub classes to decide when to recycle differently.
  public void doRecycleOnRead() {
    recycle();
  }

  // if a subclass calls recycleOutputStream, then they need to override this method to return true
  protected boolean isOutputStreamRecycled() {
    return false;
  }

  protected void recycleOutputStream() {
    if (out != null) {
      out.recycle();
    }
  }

  /**
   * Subclasses *really* must implement this to set appropriate instance variables with the value of the given name.
   * Return false if the given name is unknown to your message class
   * 
   * @param name
   */
  protected boolean hydrateValue(final byte name) throws IOException {
    if (false) { throw new IOException("silence compiler warning"); }
    return false;
  }

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
    return ((NodeIDSerializer) getObject(new NodeIDSerializer())).getNodeID();
  }

  protected LockID getLockIDValue() throws IOException {
    return ((LockIDSerializer) getObject(new LockIDSerializer())).getLockID();
  }

  protected Object getObject(final TCSerializable target) throws IOException {
    return target.deserializeFrom(bbis);
  }

  protected String getStringValue() throws IOException {
    return bbis.readString();
  }

  protected byte[] getBytesArray() throws IOException {
    int length = bbis.readInt();
    byte bytes[] = new byte[length];
    int off = 0;
    while (length > 0) {
      int read = bbis.read(bytes, off, length);
      length -= read;
      off += read;
    }
    return bytes;
  }

  protected void putNVPair(final byte name, final boolean value) {
    nvCount++;
    out.write(name);
    out.writeBoolean(value);
  }

  protected void putNVPair(final byte name, final NVPair nvPair, final ObjectStringSerializer serializer) {
    nvCount++;
    out.write(name);
    nvPair.serializeTo(out, serializer);
  }

  protected void putNVPair(final byte name, final byte value) {
    nvCount++;
    out.write(name);
    out.writeByte(value);
  }

  protected void putNVPair(final byte name, final char value) {
    nvCount++;
    out.write(name);
    out.writeChar(value);
  }

  protected void putNVPair(final byte name, final double value) {
    nvCount++;
    out.write(name);
    out.writeDouble(value);
  }

  protected void putNVPair(final byte name, final float value) {
    nvCount++;
    out.write(name);
    out.writeFloat(value);
  }

  protected void putNVPair(final byte name, final int value) {
    nvCount++;
    out.write(name);
    out.writeInt(value);
  }

  protected void putNVPair(final byte name, final long value) {
    nvCount++;
    out.write(name);
    out.writeLong(value);
  }

  protected void putNVPair(final byte name, final short value) {
    nvCount++;
    out.write(name);
    out.writeShort(value);
  }

  protected void putNVPair(final byte name, final String value) {
    nvCount++;
    out.write(name);
    out.writeString(value);
  }

  protected void putNVPair(final byte name, final NodeID nodeID) {
    nvCount++;
    out.write(name);
    new NodeIDSerializer(nodeID).serializeTo(out);
  }

  protected void putNVPair(final byte name, final LockID lid) {
    nvCount++;
    out.write(name);
    new LockIDSerializer(lid).serializeTo(out);
  }

  protected void putNVPair(final byte name, final TCSerializable object) {
    nvCount++;
    out.write(name);
    object.serializeTo(out);
  }

  protected void putNVPair(final byte name, final TCByteBuffer[] data) {
    nvCount++;
    out.write(name);
    out.write(data);
  }

  protected void putNVPair(final byte name, final byte[] bytes) {
    nvCount++;
    out.write(name);
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  public ChannelID getChannelID() {
    return channel.getChannelID();
  }

  public MessageChannel getChannel() {
    return channel;
  }

  /*
   * (non-Javadoc)
   * @see com.tc.net.protocol.tcm.ApplicationMessage#send()
   */
  public void send() {
    if (isSent.attemptSet()) {
      dehydrate();
      basicSend();
    }
  }

  private void basicSend() {
    channel.send(this);
    monitor.newOutgoingMessage(this);
  }

  /*
   * send with payload from a dehydrated message
   */
  public void cloneAndSend(final TCMessageImpl message) {
    if (isSent.attemptSet()) {
      dehydrate(message.getPayload());
      basicSend();
    }
  }

  public NodeID getSourceNodeID() {
    return isOutgoing ? channel.getLocalNodeID() : channel.getRemoteNodeID();
  }

  public NodeID getDestinationNodeID() {
    return isOutgoing ? channel.getRemoteNodeID() : channel.getLocalNodeID();
  }

}
