/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime.logging;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;

public class LongGCLogger implements MemoryEventsListener {

  private static final TCLogger logger = TCLogging.getLogger(LongGCLogger.class);
  private final long            gcTimeout;
  private MemoryUsage           lastMemoryUsage;

  public LongGCLogger(final long gcTimeOut) {
    this.gcTimeout = gcTimeOut;
  }

  public void memoryUsed(MemoryUsage currentUsage, boolean recommendOffheap) {
    if (lastMemoryUsage == null) {
      lastMemoryUsage = currentUsage;
      return;
    }
    long countDiff = currentUsage.getCollectionCount() - lastMemoryUsage.getCollectionCount();
    long timeDiff = currentUsage.getCollectionTime() - lastMemoryUsage.getCollectionTime();
    if (countDiff > 0 && timeDiff > gcTimeout) {

      TerracottaOperatorEvent tcEvent;
      if (!recommendOffheap) {
        tcEvent = TerracottaOperatorEventFactory.createLongGCOperatorEvent(new Object[] { gcTimeout, countDiff,
            timeDiff });
      } else {
        tcEvent = TerracottaOperatorEventFactory.createLongGCAndRecommendationOperatorEvent(new Object[] { gcTimeout,
            countDiff, timeDiff });
      }

      fireLongGCEvent(tcEvent);
    }
    lastMemoryUsage = currentUsage;
  }

  protected void fireLongGCEvent(TerracottaOperatorEvent tcEvent) {
    logger.warn(tcEvent.getEventMessage());
  }
}
