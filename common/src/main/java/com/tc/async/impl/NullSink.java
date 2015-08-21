/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.stats.Stats;

/**
 * @author steve
 */
public class NullSink<EC> implements Sink<EC> {
  public NullSink() {
    //
  }

  @Override
  public void addSingleThreaded(EC context) {
    //
  }

  @Override
  public void addMultiThreaded(EC context) {
    //
  }

  @Override
  public void addSpecialized(SpecializedEventContext specialized) {
    // Not expected in the null case.
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enableStatsCollection(boolean enable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stats getStats(long frequency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resetStats() {
    throw new UnsupportedOperationException();
    
  }

}
