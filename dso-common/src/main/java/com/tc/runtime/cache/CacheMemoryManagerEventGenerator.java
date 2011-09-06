/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime.cache;

import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCMemoryManager;

public class CacheMemoryManagerEventGenerator implements MemoryEventsListener {

  private final CacheMemoryEventsListener listener;
  private CacheMemoryEventType            currentState;
  private MemoryUsage                     lastReported;
  private final int                       threshold;
  private final int                       criticalThreshold;
  private final int                       leastCount;
  private final boolean                   isOldGen;

  public CacheMemoryManagerEventGenerator(int threshold, int criticalThreshold, int leastCount,
                                          TCMemoryManager manager, CacheMemoryEventsListener listener) {
    verifyInput(threshold, criticalThreshold, leastCount);
    this.listener = listener;
    this.threshold = threshold;
    this.criticalThreshold = criticalThreshold;
    this.leastCount = leastCount;
    manager.registerForMemoryEvents(this);
    this.isOldGen = manager.isMonitorOldGenOnly();
    this.currentState = CacheMemoryEventType.BELOW_THRESHOLD;
  }

  private void verifyInput(int thrhdl, int cricThrhld, int lc) {
    if (thrhdl <= 0 || thrhdl >= 100) { throw new AssertionError("Used Threshold should be > 0 && < 100 : " + thrhdl
                                                                 + " Outside range"); }
    if (cricThrhld <= 0 || cricThrhld >= 100) { throw new AssertionError(
                                                                         "Critical Used Threshold should be > 0 && < 100 : "
                                                                             + cricThrhld + " Outside range"); }
    if (thrhdl > cricThrhld) { throw new AssertionError("Used Threshold should be <= Critical Used Threshold : "
                                                        + thrhdl + " <= " + cricThrhld); }
    if (lc <= 0 || lc >= 100) { throw new AssertionError("Least Count should be > 0 && < 100 : " + lc
                                                         + " Outside range"); }
  }

  public void memoryUsed(MemoryUsage usage, boolean recommendOffheap) {
    int usedPercentage = usage.getUsedPercentage();
    if (usedPercentage < threshold) {
      if (currentState != CacheMemoryEventType.BELOW_THRESHOLD) {
        // Send only 1 BELOW_THRESHOLD event
        fire(CacheMemoryEventType.BELOW_THRESHOLD, usage);
      }
    } else if (usedPercentage >= criticalThreshold) {
      if (!isOldGen || currentState != CacheMemoryEventType.ABOVE_CRITICAL_THRESHOLD
          || isLeastCountReached(usedPercentage) || isGCCompleted(usage)) {
        // Send an event every time if we are monitoring the entire heap or else if we are monitoring only old gen
        // then send an event only if greater than least count or if we just reached ABOVE_CRITICAL_THRESHOLD or if a gc
        // took place
        fire(CacheMemoryEventType.ABOVE_CRITICAL_THRESHOLD, usage);
      }
    } else if (currentState != CacheMemoryEventType.ABOVE_THRESHOLD || isLeastCountReached(usedPercentage)) {
      fire(CacheMemoryEventType.ABOVE_THRESHOLD, usage);
    }
  }

  private boolean isGCCompleted(MemoryUsage mu) {
    return lastReported.getCollectionCount() < mu.getCollectionCount();
  }

  private boolean isLeastCountReached(int usedPercentage) {
    return (Math.abs(usedPercentage - lastReported.getUsedPercentage()) >= leastCount);
  }

  private void fire(CacheMemoryEventType type, MemoryUsage mu) {
    listener.memoryUsed(type, mu);
    currentState = type;
    lastReported = mu;
  }
}
