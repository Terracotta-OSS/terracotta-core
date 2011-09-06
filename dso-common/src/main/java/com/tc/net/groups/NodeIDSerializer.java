/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.StripeID;

import java.io.IOException;

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

  private static NodeID getImpl(byte type) {
    switch (type) {
      case NodeID.CLIENT_NODE_TYPE:
        return new ClientID();
      case NodeID.SERVER_NODE_TYPE:
        return new ServerID();
      case NodeID.GROUP_NODE_TYPE:
        return new GroupID();
      case NodeID.STRIPE_NODE_TYPE:
        return new StripeID();
      default:
        throw new AssertionError("Unknown type : " + type);
    }
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    byte type = serialInput.readByte();
    this.nodeID = getImpl(type);
    this.nodeID.deserializeFrom(serialInput);
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeByte(this.nodeID.getNodeType());
    this.nodeID.serializeTo(serialOutput);
  }
}
