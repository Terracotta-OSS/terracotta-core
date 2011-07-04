/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

  public boolean addLossy(EventContext context) {
    return false;
  }

  public void addMany(Collection contexts) {
    //
  }

  public void add(EventContext context) {
    //
  }

  public void setAddPredicate(AddPredicate predicate) {
    //
  }

  public AddPredicate getPredicate() {
    return null;
  }

  public int size() {
    return 0;
  }

  public void clear() {
    throw new ImplementMe();
  }

  public void enableStatsCollection(boolean enable) {
    throw new ImplementMe();
  }

  public Stats getStats(long frequency) {
    throw new ImplementMe();
  }

  public Stats getStatsAndReset(long frequency) {
    throw new ImplementMe();
  }

  public boolean isStatsCollectionEnabled() {
    throw new ImplementMe();
  }

  public void resetStats() {
    throw new ImplementMe();
    
  }

}