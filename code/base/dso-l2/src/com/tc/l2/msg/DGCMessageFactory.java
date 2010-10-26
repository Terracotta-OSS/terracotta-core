/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.ObjectIDSet;

public class DGCMessageFactory {

  public static final int DGC_START  = 0;
  public static final int DGC_RESULT = 1;
  public static final int DGC_CANCEL = 2;

  public static DGCStatusMessage createDGCStartMessage(GarbageCollectionInfo gcInfo) {
    return new DGCStatusMessage(DGC_START, gcInfo);
  }

  public static DGCResultMessage createDGCResultMessage(GarbageCollectionInfo gcInfo, ObjectIDSet deleted) {
    return new DGCResultMessage(DGC_RESULT, gcInfo, deleted);
  }

  public static DGCStatusMessage createDGCCancelMessage(GarbageCollectionInfo gcInfo) {
    return new DGCStatusMessage(DGC_CANCEL, gcInfo);
  }

}
