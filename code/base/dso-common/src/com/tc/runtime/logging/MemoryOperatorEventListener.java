/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime.logging;

import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;

public class MemoryOperatorEventListener implements MemoryEventsListener {

  private final int                           critcalThreshold;
  private MemoryUsage                         lastMemoryUsage;
  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public MemoryOperatorEventListener(int criticalThreshold) {
    this.critcalThreshold = criticalThreshold;
  }

  public void memoryUsed(MemoryUsage currentUsage) {
    if (lastMemoryUsage == null) {
      lastMemoryUsage = currentUsage;
      return;
    }
    long countDiff = currentUsage.getCollectionCount() - lastMemoryUsage.getCollectionCount();
    if (countDiff > 0 && currentUsage.getUsedPercentage() >= this.critcalThreshold) {
      operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createHighMemoryUsageEvent(currentUsage
          .getUsedPercentage()));
    }
    this.lastMemoryUsage = currentUsage;
  }

}
