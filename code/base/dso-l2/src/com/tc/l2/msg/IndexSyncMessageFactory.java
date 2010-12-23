/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.groups.GroupMessage;

public class IndexSyncMessageFactory {

  public static GroupMessage createIndexCheckSyncRequestMessage() {
    return new IndexCheckSyncMessage(IndexCheckSyncMessage.REQUEST);
  }

  public static GroupMessage createIndexCheckSyncResponseMessage(IndexCheckSyncMessage initiatedMsg, boolean syncIndex) {
    return new IndexCheckSyncMessage(initiatedMsg.getMessageID(), IndexCheckSyncMessage.RESPONSE, syncIndex);
  }

  public static GroupMessage createIndexCheckSyncFailedMessage(IndexCheckSyncMessage initiatedMsg) {
    return new IndexCheckSyncMessage(initiatedMsg.getMessageID(), IndexCheckSyncMessage.FAILED_RESPONSE);
  }

  public static IndexSyncMessage createIndexSyncMessage(String cacheName, String fileName, int length, byte[] fileData,
                                                        long sequenceID) {
    IndexSyncMessage msg = new IndexSyncMessage(IndexSyncMessage.INDEX_SYNC_TYPE);
    msg.initialize(cacheName, fileName, length, fileData, sequenceID);
    return msg;
  }

  public static IndexSyncCompleteMessage createIndexSyncCompleteMessage(long sID) {
    IndexSyncCompleteMessage msg = new IndexSyncCompleteMessage(IndexSyncCompleteMessage.INDEX_SYNC_COMPLETE_TYPE);
    msg.initialize(sID);
    return msg;
  }

}
