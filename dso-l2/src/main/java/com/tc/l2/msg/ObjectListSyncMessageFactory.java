/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.net.groups.GroupMessage;
import com.tc.util.State;

import java.util.Set;

public class ObjectListSyncMessageFactory {

  public static GroupMessage createObjectListSyncRequestMessage() {
    return new ObjectListSyncMessage(ObjectListSyncMessage.REQUEST);
  }

  public static GroupMessage createObjectListSyncResponseMessage(ObjectListSyncMessage initiatingMsg,
                                                                 State currentState, Set knownIDs, boolean isCleanDB,
                                                                 final MonitoredResource resource) {
    return new ObjectListSyncMessage(initiatingMsg.getMessageID(), ObjectListSyncMessage.RESPONSE, currentState,
                                     knownIDs, isCleanDB, resource.getType().name(), resource.getTotal());
  }

  public static GroupMessage createObjectListSyncFailedResponseMessage(ObjectListSyncMessage initiatingMsg) {
    return new ObjectListSyncMessage(initiatingMsg.getMessageID(), ObjectListSyncMessage.FAILED_RESPONSE);
  }

}
