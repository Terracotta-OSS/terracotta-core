/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.groups.NodeID;

public class ObjectSyncCompleteMessageFactory {

  public static ObjectSyncCompleteMessage createObjectSyncCompleteMessageFor(NodeID nodeID, long sequence) {
    return new ObjectSyncCompleteMessage(ObjectSyncCompleteMessage.OBJECT_SYNC_COMPLETE, sequence);
  }

}
