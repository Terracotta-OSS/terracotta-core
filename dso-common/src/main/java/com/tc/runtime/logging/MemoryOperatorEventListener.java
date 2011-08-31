/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime.logging;

import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;

public class MemoryOperatorEventListener implements MemoryEventsListener {
  private static final int                    TIME_INTERVAL_BETWEEN_TWO_EVENTS = 1800 * 1000;

  private final int                           critcalThreshold;
  private boolean                             canFireOpEvent                   = true;
  private long                                lastEventFireTime                = 0;
  private final TerracottaOperatorEventLogger operatorEventLogger              = TerracottaOperatorEventLogging
                                                                                   .getEventLogger();

  public MemoryOperatorEventListener(int criticalThreshold) {
    this.critcalThreshold = criticalThreshold;
  }

  public void memoryUsed(MemoryUsage currentUsage, boolean recommendOffheap) {
    if (!canFireOpEvent && currentUsage.getUsedPercentage() < this.critcalThreshold) {
      this.canFireOpEvent = true;
    }

    if (canFireOpEvent && (System.currentTimeMillis() - this.lastEventFireTime >= TIME_INTERVAL_BETWEEN_TWO_EVENTS)
        && currentUsage.getUsedPercentage() >= this.critcalThreshold) {
      operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createHighMemoryUsageEvent(currentUsage
          .getUsedPercentage(), this.critcalThreshold));
      this.lastEventFireTime = System.currentTimeMillis();
      this.canFireOpEvent = false;
    }
  }
}
