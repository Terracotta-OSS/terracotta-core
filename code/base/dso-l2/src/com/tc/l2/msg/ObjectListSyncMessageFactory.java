/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.groups.GroupMessage;

import java.util.Set;

public class ObjectListSyncMessageFactory {

  public static GroupMessage createObjectListSyncRequestMessage() {
    return new ObjectListSyncMessage(ObjectListSyncMessage.REQUEST);
  }

  public static GroupMessage createObjectListSyncResponseMessage(ObjectListSyncMessage initiatingMsg, Set knownIDs) {
    return new ObjectListSyncMessage(initiatingMsg.getMessageID(), ObjectListSyncMessage.RESPONSE, knownIDs);
  }

}
