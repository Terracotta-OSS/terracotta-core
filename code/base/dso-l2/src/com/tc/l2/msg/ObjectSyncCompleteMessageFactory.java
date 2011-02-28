/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.NodeID;

public class ObjectSyncCompleteMessageFactory {

  public static ObjectSyncCompleteMessage createObjectSyncCompleteMessageFor(NodeID nodeID, long sequence) {
    return new ObjectSyncCompleteMessage(ObjectSyncCompleteMessage.OBJECT_SYNC_COMPLETE, sequence);
  }

  public static ObjectSyncCompleteAckMessage createObjectSyncCompleteAckMessage(NodeID nodeID) {
    return new ObjectSyncCompleteAckMessage(ObjectSyncCompleteAckMessage.OBJECT_SYNC_COMPLETE_ACK, nodeID);
  }
}
