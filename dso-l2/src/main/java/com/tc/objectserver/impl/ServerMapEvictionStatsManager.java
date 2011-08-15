/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogging;

import java.util.concurrent.atomic.AtomicLong;

public class ServerMapEvictionStatsManager {

  private static final TCLogger logger                      = TCLogging.getLogger(ServerMapEvictionStatsManager.class);

  private long                  periodicEvictionStartTime;
  private long                  periodicEvictionEndTime;
  private final AtomicLong      segmentsUnderThresholdCount = new AtomicLong();
  private final AtomicLong      segmentsOverThresholdCount  = new AtomicLong();
  private final AtomicLong      totalOvershoot              = new AtomicLong();
  private final AtomicLong      totalSamplesRequested       = new AtomicLong();

  private final AtomicLong      segmentsWithEvictionCount   = new AtomicLong();
  private final AtomicLong      evictedEntriesCount         = new AtomicLong();

  private void resetCounters() {
    periodicEvictionStartTime = now();
    periodicEvictionEndTime = now();
    segmentsUnderThresholdCount.set(0);
    segmentsOverThresholdCount.set(0);
    totalOvershoot.set(0);
    totalSamplesRequested.set(0);

    segmentsWithEvictionCount.set(0);
    evictedEntriesCount.set(0);
  }

  public void periodicEvictionStarted() {
    periodicEvictionStartTime = now();
  }

  private long now() {
    return System.currentTimeMillis();
  }

  public void evictionNotRequired(ObjectID oid, EvictableMap ev, int targetMaxTotalCount, int currentSize) {
    segmentsUnderThresholdCount.incrementAndGet();
  }

  public void evictionRequested(ObjectID oid, EvictableMap ev, int targetMaxTotalCount, int overshoot, int samplesSize) {
    segmentsOverThresholdCount.incrementAndGet();
    totalOvershoot.addAndGet(overshoot);
    totalSamplesRequested.addAndGet(samplesSize);
  }

  public void entriesEvicted(ObjectID oid, int overshoot, int samplesSize, int numEvictedEntries) {
    segmentsWithEvictionCount.incrementAndGet();
    evictedEntriesCount.addAndGet(numEvictedEntries);
  }

  public void periodicEvictionFinished() {
    periodicEvictionEndTime = now();

    fireOperatorEvent();
    log("Time taken (msecs): " + (periodicEvictionEndTime - periodicEvictionStartTime)
        + ", Number of segments under threshold: " + segmentsUnderThresholdCount.get()
        + ", Number of segments over threshold: " + segmentsOverThresholdCount.get() + ", Total overshoot: "
        + totalOvershoot.get() + ", Total number of samples requested: " + totalSamplesRequested.get()
        + ", Number of segments where eviction happened: " + segmentsWithEvictionCount.get()
        + ", Total number of evicted entries: " + evictedEntriesCount.get());

    resetCounters();
  }

  private void log(String msg) {
    logger.info("Server Map Periodic eviction - " + msg);
  }

  private void fireOperatorEvent() {
    TerracottaOperatorEvent event = TerracottaOperatorEventFactory
        .createServerMapEvictionOperatorEvent(getServerMapEvictionOperatorEventArguments());
    TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(event);
  }

  /**
   * Format of message is: DCV2 Eviction - Time taken (msecs)={0}, Number of entries evicted={1}, Number of segments
   * over threshold={2}, Total Overshoot={3}
   */
  private Object[] getServerMapEvictionOperatorEventArguments() {
    return new Object[] { (periodicEvictionEndTime - periodicEvictionStartTime), evictedEntriesCount.get(),
        segmentsOverThresholdCount.get(), totalOvershoot.get() };
  }
}
