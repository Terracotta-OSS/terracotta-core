/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * This is a helper class to hide the serialization and deserialization of NodeID implementations from external world.
 * Having it here makes it easy to abstract the differences from everywhere else. The downside is that when a new
 * implementation comes around this class needs to be updated.
 */
public class NodeIDSerializer implements TCSerializable {

  private NodeID            nodeID;

  public NodeIDSerializer() {
    // NOP
  }

  public NodeIDSerializer(NodeID nodeID) {
    this.nodeID = nodeID;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public static void writeNodeID(NodeID n, ObjectOutput out) throws IOException {
    byte type = n.getType();
    out.writeByte(type);
    n.writeExternal(out);
  }

  public static NodeID readNodeID(ObjectInput in) throws IOException, ClassNotFoundException {
    byte type = in.readByte();
    NodeID n = getImpl(type);
    n.readExternal(in);
    return n;
  }

  private static NodeID getImpl(byte type) {
    switch (type) {
      case NodeID.L1_NODE_TYPE:
        return new ClientID();
      case NodeID.L2_NODE_TYPE:
        return new NodeIDImpl();
      default:
        throw new AssertionError("Unknown type : " + type);
    }
  }

  // XXX:: These are not very efficient ways to serialize and deserialize NodeIDs, this is here coz it is used by two
  // different stack implementation
  public byte[] getBytes(NodeID n) {
    try {
      ByteArrayOutputStream bao = new ByteArrayOutputStream(64);
      // XXX::NOTE:: We are using TCObjectOutputStream which can only serialize known types. @see writeObject()
      TCObjectOutputStream tos = new TCObjectOutputStream(bao);
      writeNodeID(n, tos);
      tos.close();
      return bao.toByteArray();
    } catch (IOException ioe) {
      throw new AssertionError(ioe);
    }
  }

  // XXX:: These are not very efficient ways to serialize and deserialize NodeIDs, this is here coz it is used by two
  // different stack implementation
  public NodeID createFrom(byte[] data) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      TCObjectInputStream tci = new TCObjectInputStream(bais);
      return readNodeID(tci);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    byte type = serialInput.readByte();
    this.nodeID = getImpl(type);
    this.nodeID.deserializeFrom(serialInput);
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeByte(this.nodeID.getType());
    this.nodeID.serializeTo(serialOutput);
  }
}
