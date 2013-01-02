/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.stats.counter.sampled.SampledCounter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements the object manager stats
 */
public class ObjectManagerStatsImpl implements ObjectManagerStatsListener, ObjectManagerStats {

  private final AtomicLong     objectsCreated = new AtomicLong();
  private final SampledCounter newObjectCounter;

  public ObjectManagerStatsImpl(SampledCounter newObjectCounter) {
    this.newObjectCounter = newObjectCounter;
  }

  @Override
  public void newObjectCreated() {
    this.objectsCreated.incrementAndGet();
    this.newObjectCounter.increment();
  }

  @Override
  public long getTotalObjectsCreated() {
    return this.objectsCreated.get();
  }

}
