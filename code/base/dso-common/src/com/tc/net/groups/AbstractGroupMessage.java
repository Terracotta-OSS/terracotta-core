/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractGroupMessage implements GroupMessage {

  private static long      nextID           = 0;

  private int              type;
  private MessageID        id;
  private MessageID        requestID;

  private transient NodeID messageOrginator = NodeID.NULL_ID;

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

  public final void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    type = in.readInt();
    id = new MessageID(in.readLong());
    requestID = new MessageID(in.readLong());
    basicReadExternal(type, in);

  }

  public final void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(type);
    out.writeLong(id.toLong());
    out.writeLong(requestID.toLong());
    basicWriteExternal(type, out);
  }

  protected abstract void basicWriteExternal(int msgType, ObjectOutput out) throws IOException;

  protected abstract void basicReadExternal(int msgType, ObjectInput in) throws IOException, ClassNotFoundException;

  protected void writeObjectStringSerializer(ObjectOutput out, ObjectStringSerializer lserializer) throws IOException {
    TCByteBufferOutputStream tcbo = new TCByteBufferOutputStream();
    lserializer.serializeTo(tcbo);
    writeByteBuffers(out, tcbo.toArray());
    tcbo.recycle();
  }
  
  protected void writeByteBuffers(ObjectOutput out, TCByteBuffer[] buffers) throws IOException {
    out.writeInt(buffers.length);
    for (int i = 0; i < buffers.length; i++) {
      TCByteBuffer buffer = buffers[i];
      int length = buffer.limit();
      out.writeInt(length);
      out.write(buffer.array(), buffer.arrayOffset(), length);
    }
  }


  protected ObjectStringSerializer readObjectStringSerializer(ObjectInput in) throws IOException {
    TCByteBuffer buffers[] = readByteBuffers(in);
    ObjectStringSerializer lserializer = new ObjectStringSerializer();
    lserializer.deserializeFrom(new TCByteBufferInputStream(buffers));
    return lserializer;
  }
  
  protected TCByteBuffer[] readByteBuffers(ObjectInput in) throws IOException {
    int size = in.readInt();
    TCByteBuffer buffers[] = new TCByteBuffer[size];
    for (int i = 0; i < buffers.length; i++) {
      int length = in.readInt();
      byte bytes[] = new byte[length];
      int start = 0;
      while (length > 0) {
        int read = in.read(bytes, start, length);
        start += read;
        length -= read;
      }
      buffers[i] = TCByteBufferFactory.wrap(bytes);
    }
    return buffers;
  }
  
  protected void writeObjectIDS(ObjectOutput out, Set oids) throws IOException {
    out.writeInt(oids.size());
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      out.writeLong(oid.toLong());
    }
  }
  
  protected Set readObjectIDS(ObjectInput in, Set oids) throws IOException {
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      oids.add(new ObjectID(in.readLong()));
    }
    return oids;
  }
}
