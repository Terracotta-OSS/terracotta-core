/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.ObjectIDSet;

public class GCResultMessageFactory {

  public static GCResultMessage createGCResultMessage(GarbageCollectionInfo gcInfo,  ObjectIDSet deleted) {
    return new GCResultMessage(GCResultMessage.GC_RESULT, gcInfo, deleted);
  }

}
