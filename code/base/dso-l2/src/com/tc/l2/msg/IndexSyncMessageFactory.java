/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;


public class IndexSyncMessageFactory {

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
