/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.NodeID;
import com.tc.net.groups.MessageID;

public class IndexSyncMessageFactory {

  public static IndexSyncStartMessage createIndexSyncStartMessage(final long sequenceID) {
    IndexSyncStartMessage msg = new IndexSyncStartMessage(IndexSyncStartMessage.INDEX_SYNC_START_TYPE);
    msg.initialize(sequenceID);
    return msg;
  }

  public static IndexSyncMessage createIndexSyncMessage(String cacheName, String fileName, byte[] fileData,
                                                        long sequenceID, boolean isTCFile, boolean isLast) {
    IndexSyncMessage msg = new IndexSyncMessage(IndexSyncMessage.INDEX_SYNC_TYPE);
    msg.initialize(cacheName, fileName, fileData, sequenceID, isTCFile, isLast);
    return msg;
  }

  public static IndexSyncCompleteMessage createIndexSyncCompleteMessage(final long sequenceID) {
    IndexSyncCompleteMessage msg = new IndexSyncCompleteMessage(IndexSyncCompleteMessage.INDEX_SYNC_COMPLETE_TYPE);
    msg.initialize(sequenceID);
    return msg;
  }

  public static IndexSyncAckMessage createIndexSyncAckMessage(MessageID requestID, int amount) {
    IndexSyncAckMessage msg = new IndexSyncAckMessage(IndexSyncAckMessage.INDEX_SYNC_ACK_TYPE, requestID);
    msg.initialize(amount);
    return msg;
  }

  public static IndexSyncCompleteAckMessage createIndexSyncCompleteAckMessage(NodeID nodeID) {
    return new IndexSyncCompleteAckMessage(IndexSyncCompleteAckMessage.INDEX_SYNC_COMPLETE_ACK_TYPE, nodeID);
  }

}
