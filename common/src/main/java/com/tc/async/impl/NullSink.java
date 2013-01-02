/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.stats.Stats;

import java.util.Collection;

/**
 * @author steve
 */
public class NullSink implements Sink {
  public NullSink() {
    //
  }

  @Override
  public boolean addLossy(EventContext context) {
    return false;
  }

  @Override
  public void addMany(Collection contexts) {
    //
  }

  @Override
  public void add(EventContext context) {
    //
  }

  @Override
  public void setAddPredicate(AddPredicate predicate) {
    //
  }

  @Override
  public AddPredicate getPredicate() {
    return null;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public void clear() {
    throw new ImplementMe();
  }

  @Override
  public void enableStatsCollection(boolean enable) {
    throw new ImplementMe();
  }

  @Override
  public Stats getStats(long frequency) {
    throw new ImplementMe();
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    throw new ImplementMe();
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    throw new ImplementMe();
  }

  @Override
  public void resetStats() {
    throw new ImplementMe();
    
  }

}
