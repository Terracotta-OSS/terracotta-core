package com.tc.async.impl;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.stats.Stats;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author orion
 */
public class MockSink implements Sink {

  public BoundedLinkedQueue queue = new BoundedLinkedQueue(Integer.MAX_VALUE);

  public EventContext take() {
    try {
      return (EventContext) this.queue.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public boolean addLossy(EventContext context) {
    if (queue.size() < 1) {
      try {
        this.queue.put(context);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
      return true;
    } else {
      return false;
    }
  }

  public void addMany(Collection contexts) {
    for (Iterator i = contexts.iterator(); i.hasNext();)
      try {
        this.queue.put(contexts);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
  }

  public void add(EventContext context) {
    try {
      this.queue.put(context);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public void setAddPredicate(AddPredicate predicate) {
    throw new ImplementMe();
  }

  public AddPredicate getPredicate() {
    throw new ImplementMe();
  }

  public int size() {
    return this.queue.size();
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