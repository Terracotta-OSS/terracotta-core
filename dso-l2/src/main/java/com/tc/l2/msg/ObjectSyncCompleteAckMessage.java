/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

public class ObjectSyncCompleteAckMessage extends AbstractGroupMessage implements EventContext {

  public static final int OBJECT_SYNC_COMPLETE_ACK = 0x00;
  private volatile NodeID nodeID;

  // To make serialization happy
  public ObjectSyncCompleteAckMessage() {
    super(-1);
  }

  public ObjectSyncCompleteAckMessage(int type, NodeID nodeID) {
    super(type);
    this.nodeID = nodeID;
  }

  public NodeID getDestinationNodeID() {
    return this.nodeID;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) {
    Assert.assertEquals(OBJECT_SYNC_COMPLETE_ACK, getType());
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(OBJECT_SYNC_COMPLETE_ACK, getType());
  }

}
