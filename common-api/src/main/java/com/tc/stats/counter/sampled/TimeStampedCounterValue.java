/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.counter.sampled;

import java.io.Serializable;

/**
 * A counter value at a particular time instance
 */
public class TimeStampedCounterValue implements Serializable {
  private final long counterValue;
  private final long timestamp;

  public TimeStampedCounterValue(long timestamp, long value) {
    this.timestamp = timestamp;
    this.counterValue = value;
  }

  public long getCounterValue() {
    return this.counterValue;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public String toString() {
    return "value: " + this.counterValue + ", timestamp: " + this.timestamp;
  }

}
