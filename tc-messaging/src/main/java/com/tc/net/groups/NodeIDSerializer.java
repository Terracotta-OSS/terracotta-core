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
public class NodeIDSerializer implements TCSerializable<NodeIDSerializer> {

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

  @Override
  public NodeIDSerializer deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    byte type = serialInput.readByte();
    this.nodeID = getImpl(type);
    this.nodeID.deserializeFrom(serialInput);
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeByte(this.nodeID.getNodeType());
    this.nodeID.serializeTo(serialOutput);
  }
}
