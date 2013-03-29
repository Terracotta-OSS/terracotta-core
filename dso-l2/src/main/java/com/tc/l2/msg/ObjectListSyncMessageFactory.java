/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.util.State;

public class ObjectListSyncMessageFactory {

  public static ObjectListSyncMessage createObjectListSyncRequestMessage() {
    return new ObjectListSyncMessage(ObjectListSyncMessage.REQUEST);
  }

  public static ObjectListSyncMessage createObjectListSyncResponseMessage(ObjectListSyncMessage initiatingMsg,
                                                                 State currentState, boolean syncAllowed, boolean isCleanDB,
                                                                 final boolean offheapEnabled, final long totalSize) {
    return new ObjectListSyncMessage(initiatingMsg.getMessageID(), ObjectListSyncMessage.RESPONSE, currentState,
        syncAllowed, isCleanDB, offheapEnabled, totalSize);
  }

  public static ObjectListSyncMessage createObjectListSyncFailedResponseMessage(ObjectListSyncMessage initiatingMsg) {
    return new ObjectListSyncMessage(initiatingMsg.getMessageID(), ObjectListSyncMessage.FAILED_RESPONSE);
  }

}
