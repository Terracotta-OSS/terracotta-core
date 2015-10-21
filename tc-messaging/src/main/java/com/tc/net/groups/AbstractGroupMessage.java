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

  @Override
  final public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(this.type);
    serialOutput.writeLong(this.id.toLong());
    serialOutput.writeLong(this.requestID.toLong());
    basicSerializeTo(serialOutput);
  }

  @Override
  final public GroupMessage deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.type = serialInput.readInt();
    this.id = new MessageID(serialInput.readLong());
    this.requestID = new MessageID(serialInput.readLong());
    basicDeserializeFrom(serialInput);
    return this;
  }

  abstract protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException;

  abstract protected void basicSerializeTo(TCByteBufferOutput out);

  @Override
  public boolean isRecycleOnRead(TCMessageImpl message) {
    return true;
  }

  private static final synchronized MessageID getNextID() {
    return new MessageID(nextID++);
  }

  @Override
  public int getType() {
    return this.type;
  }

  @Override
  public MessageID getMessageID() {
    return this.id;
  }

  @Override
  public MessageID inResponseTo() {
    return this.requestID;
  }

  @Override
  public void setMessageOrginator(NodeID n) {
    this.messageOrginator = n;
  }

  @Override
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
