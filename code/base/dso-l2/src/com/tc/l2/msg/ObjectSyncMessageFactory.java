/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.context.ManagedObjectSyncContext;

public class ObjectSyncMessageFactory {

  public static ObjectSyncMessage createObjectSyncMessageFrom(ManagedObjectSyncContext mosc) {
    ObjectSyncMessage msg = new ObjectSyncMessage(ObjectSyncMessage.MANAGED_OBJECT_SYNC_TYPE);
    msg.initialize(mosc.getLookupIDs(), mosc.getDNACount(), mosc.getSerializedDNAs(), mosc.getObjectSerializer(), mosc
        .getRootsMap(), mosc.getSequenceID());
    return msg;
  }

}
