/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime.logging;

import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;

public class LongGCLogger implements MemoryEventsListener {

  private final long                          gcTimeout;
  private MemoryUsage                         lastMemoryUsage;
  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public LongGCLogger(final long gcTimeOut) {
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
      fireLongGCOperatorEvent(LongGCEventType.LONG_GC, countDiff, timeDiff);
    }
    lastMemoryUsage = currentUsage;
  }

  private void fireLongGCOperatorEvent(LongGCEventType type, long collectionCountDiff, long collectionTimeDiff) {
    TerracottaOperatorEvent tcEvent = TerracottaOperatorEventFactory.createLongGCOperatorEvent(new Object[] {
        gcTimeout, collectionCountDiff, collectionTimeDiff });
    operatorEventLogger.fireOperatorEvent(tcEvent);
  }
}
