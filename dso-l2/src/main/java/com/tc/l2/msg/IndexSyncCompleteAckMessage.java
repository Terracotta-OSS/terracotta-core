/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

public class IndexSyncCompleteAckMessage extends AbstractGroupMessage implements EventContext {
  public static final int INDEX_SYNC_COMPLETE_ACK_TYPE = 0;
  private volatile NodeID nodeID;

  public IndexSyncCompleteAckMessage() {
    // Make serialization happy
    super(-1);
  }

  public IndexSyncCompleteAckMessage(final int type, NodeID nodeID) {
    super(type);
    this.nodeID = nodeID;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) {
    Assert.assertEquals(INDEX_SYNC_COMPLETE_ACK_TYPE, getType());
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(INDEX_SYNC_COMPLETE_ACK_TYPE, getType());
  }

  public NodeID getDestinatioinNodeID() {
    return this.nodeID;
  }

}
