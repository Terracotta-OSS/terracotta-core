/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.object.tx.ServerTransactionID;

public class ObjectSyncMessageFactory {

  public static ObjectSyncMessage createObjectSyncMessageFrom(ManagedObjectSyncContext mosc, ServerTransactionID sid) {
    ObjectSyncMessage msg = new ObjectSyncMessage(ObjectSyncMessage.MANAGED_OBJECT_SYNC_TYPE);
    msg.initialize(sid, mosc.getSynchedOids(), mosc.getDNACount(), mosc.getSerializedDNAs(),
                   mosc.getObjectSerializer(), mosc.getRootsMap(), mosc.getSequenceID());
    return msg;
  }

}
