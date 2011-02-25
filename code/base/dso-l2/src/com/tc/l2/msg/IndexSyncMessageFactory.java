/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

public class IndexSyncMessageFactory {

  public static IndexSyncStartMessage createIndexSyncStartMessage(final long sequenceID) {
    IndexSyncStartMessage msg = new IndexSyncStartMessage(IndexSyncStartMessage.INDEX_SYNC_START_TYPE);
    msg.initialize(sequenceID);
    return msg;
  }

  public static IndexSyncMessage createIndexSyncMessage(String cacheName, String fileName, int length, byte[] fileData,
                                                        long sequenceID) {
    IndexSyncMessage msg = new IndexSyncMessage(IndexSyncMessage.INDEX_SYNC_TYPE);
    msg.initialize(cacheName, fileName, length, fileData, sequenceID);
    return msg;
  }

  public static IndexSyncCompleteMessage createIndexSyncCompleteMessage(final long sequenceID) {
    IndexSyncCompleteMessage msg = new IndexSyncCompleteMessage(IndexSyncCompleteMessage.INDEX_SYNC_COMPLETE_TYPE);
    msg.initialize(sequenceID);
    return msg;
  }

}
