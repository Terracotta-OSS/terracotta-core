/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.stats.Stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author orion
 */
public class MockSink implements Sink {

  public List queue = new ArrayList();

  public boolean addLossy(EventContext context) {
    queue.add(context);
    return true;
  }

  public void addMany(Collection contexts) {
    queue.addAll(contexts);
  }

  public void add(EventContext context) {
    queue.add(context);
  }

  public void setAddPredicate(AddPredicate predicate) {
    throw new ImplementMe();
  }

  public AddPredicate getPredicate() {
    throw new ImplementMe();
  }

  public int size() {
    return queue.size();
  }

  public EventContext getContext(int index) {
    return (EventContext) queue.get(index);
  }

  public void turnTracingOn() {
    throw new ImplementMe();
  }

  public void turnTracingOff() {
    throw new ImplementMe();
  }

  public void clear() {
    throw new ImplementMe();

  }

  public void pause(List pauseEvents) {
    throw new ImplementMe();

  }

  public void unpause() {
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