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
    id = getNextID();
    requestID = MessageID.NULL_ID;
  }

  protected AbstractGroupMessage(int type, MessageID requestID) {
    this.type = type;
    id = getNextID();
    this.requestID = requestID;
  }

  final public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(type);
    serialOutput.writeLong(id.toLong());
    serialOutput.writeLong(requestID.toLong());
    basicSerializeTo(serialOutput);
  }

  final public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    type = serialInput.readInt();
    id = new MessageID(serialInput.readLong());
    requestID = new MessageID(serialInput.readLong());
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
    return type;
  }

  public MessageID getMessageID() {
    return id;
  }

  public MessageID inResponseTo() {
    return requestID;
  }

  public void setMessageOrginator(NodeID n) {
    this.messageOrginator = n;
  }

  public NodeID messageFrom() {
    return messageOrginator;
  }

  protected void writeByteBuffers(TCByteBufferOutput out, TCByteBuffer[] buffers) {
    int total = 0;
    for (int i = 0; i < buffers.length; i++) {
      total += buffers[i].limit();
    }
    out.writeInt(total);
    for (int i = 0; i < buffers.length; i++) {
      TCByteBuffer buffer = buffers[i];
      out.write(buffer.array(), buffer.arrayOffset(), buffer.limit());
    }
  }

  protected TCByteBuffer[] readByteBuffers(TCByteBufferInput in) throws IOException {
    int total = in.readInt();
    TCByteBuffer buffers[] = TCByteBufferFactory.getFixedSizedInstancesForLength(false, total);
    for (int i = 0; i < buffers.length; i++) {
      byte bytes[] = buffers[i].array();
      int start = 0;
      int length = Math.min(bytes.length, total);
      total -= length;
      while (length > 0) {
        int read = in.read(bytes, start, length);
        start += read;
        length -= read;
      }
      buffers[i].rewind();
    }
    return buffers;
  }

}
