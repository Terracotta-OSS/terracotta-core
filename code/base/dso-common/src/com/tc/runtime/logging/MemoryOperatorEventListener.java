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
  private int                                 lastUsedPercentage  = 0;
  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public MemoryOperatorEventListener(int criticalThreshold) {
    this.critcalThreshold = criticalThreshold;
  }

  public void memoryUsed(MemoryUsage usage) {
    if (usage.getUsedPercentage() >= this.critcalThreshold && this.lastUsedPercentage < this.critcalThreshold) {
      operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createHighMemoryUsageEvent(usage
                                                                                                      .getUsedPercentage()));
    }
    this.lastUsedPercentage = usage.getUsedPercentage();
  }

}
