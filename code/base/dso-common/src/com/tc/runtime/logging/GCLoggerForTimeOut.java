/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime.logging;

import com.tc.logging.TCLogger;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;

public class GCLoggerForTimeOut implements MemoryEventsListener {

  private long        gcTimeout;
  private MemoryUsage lastMemoryUsage;
  private TCLogger    logger;

  public GCLoggerForTimeOut(TCLogger logger, long gcTimeOut) {
    this.logger = logger;
    this.gcTimeout = gcTimeOut;
  }

  public void memoryUsed(MemoryUsage currentUsage) {
    if (lastMemoryUsage == null) {
      lastMemoryUsage = currentUsage;
      return;
    }
    long countDiff = currentUsage.getCollectionCount() - lastMemoryUsage.getCollectionCount();
    long timeDiff = currentUsage.getCollectionTime() - lastMemoryUsage.getCollectionTime();
    if (countDiff > 0 && timeDiff > gcTimeout) {
      fireEvent(GCLoggingMemoryEventType.LONG_GC, countDiff, timeDiff);
    }
    lastMemoryUsage = currentUsage;
  }

  private void fireEvent(GCLoggingMemoryEventType type, long collectionCountDiff, long collectionTimeDiff) {
    String message = "Detected Long GC > " + gcTimeout + " ms. Event Type : " + type + " GC Collection Count: "
                     + collectionCountDiff + " GC Collection Time: " + collectionTimeDiff + " ms";
    logger.warn(message);
  }
}
