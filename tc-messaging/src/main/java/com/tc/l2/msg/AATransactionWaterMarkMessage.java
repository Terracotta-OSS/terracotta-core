/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeIDSerializer;

import java.io.IOException;

public class AATransactionWaterMarkMessage extends AbstractGroupMessage implements EventContext {

  public static final int WATERMARK_BROADCAST = 0;

  private GroupID         groupID;
  private NodeID          nodeID;
  private long            highWatermark;

  // To make serialization happy
  public AATransactionWaterMarkMessage() {
    super(-1);
  }

  public AATransactionWaterMarkMessage(GroupID groupID, NodeID nodeID, long highWatermark) {
    super(WATERMARK_BROADCAST);
    this.groupID = groupID;
    this.nodeID = nodeID;
    this.highWatermark = highWatermark;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    this.groupID = new GroupID(in.readInt());
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    this.nodeID  = nodeIDSerializer.getNodeID();
    this.highWatermark = in.readLong();
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    out.writeInt(this.groupID.toInt());
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(this.nodeID);
    nodeIDSerializer.serializeTo(out);
    out.writeLong(this.highWatermark);
  }

  public GroupID getGroupID() {
    return groupID;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public long getHighWatermark() {
    return highWatermark;
  }

}
