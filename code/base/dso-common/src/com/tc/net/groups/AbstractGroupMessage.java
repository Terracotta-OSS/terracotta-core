/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.TCMessageImpl;

import java.io.IOException;

public abstract class AbstractGroupMessage implements GroupMessage {

  private static long      nextID           = 0;

  private int              type;
  private MessageID        id;
  private MessageID        requestID;

  private transient NodeID messageOrginator = ServerID.NULL_ID;

  protected AbstractGroupMessage(int type) {
    this.type = type;
    this.id = getNextID();
    this.requestID = MessageID.NULL_ID;
  }

  protected AbstractGroupMessage(int type, MessageID requestID) {
    this.type = type;
    this.id = getNextID();
    this.requestID = requestID;
  }

  final public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(this.type);
    serialOutput.writeLong(this.id.toLong());
    serialOutput.writeLong(this.requestID.toLong());
    basicSerializeTo(serialOutput);
  }

  final public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.type = serialInput.readInt();
    this.id = new MessageID(serialInput.readLong());
    this.requestID = new MessageID(serialInput.readLong());
    basicDeserializeFrom(serialInput);
    return this;
  }

  abstract protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException;

  abstract protected void basicSerializeTo(TCByteBufferOutput out);

  public boolean isRecycleOnRead(TCMessageImpl message) {
    return true;
  }

  private static final synchronized MessageID getNextID() {
    return new MessageID(nextID++);
  }

  public int getType() {
    return this.type;
  }

  public MessageID getMessageID() {
    return this.id;
  }

  public MessageID inResponseTo() {
    return this.requestID;
  }

  public void setMessageOrginator(NodeID n) {
    this.messageOrginator = n;
  }

  public NodeID messageFrom() {
    return this.messageOrginator;
  }

  protected void writeByteBuffers(TCByteBufferOutput out, TCByteBuffer[] buffers) {
    int total = 0;
    for (TCByteBuffer buffer : buffers) {
      total += buffer.remaining();
    }
    out.writeInt(total);
    for (TCByteBuffer buffer : buffers) {
      out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
    }
  }

  protected TCByteBuffer[] readByteBuffers(TCByteBufferInput in) throws IOException {
    int total = in.readInt();
    TCByteBuffer buffers[] = TCByteBufferFactory.getFixedSizedInstancesForLength(false, total);
    for (TCByteBuffer buffer : buffers) {
      byte bytes[] = buffer.array();
      int start = 0;
      int length = Math.min(bytes.length, total);
      total -= length;
      while (length > 0) {
        int read = in.read(bytes, start, length);
        start += read;
        length -= read;
      }
      buffer.rewind();
    }
    return buffers;
  }

}
