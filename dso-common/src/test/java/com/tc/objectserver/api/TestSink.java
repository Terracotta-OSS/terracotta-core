/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.stats.Stats;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author steve
 */
public class TestSink implements Sink {
  private final List queue = new LinkedList();

  public boolean addLossy(EventContext context) {
    return false;
  }

  public void addMany(Collection contexts) {
    //
  }

  public void add(EventContext context) {
    synchronized (queue) {
      queue.add(context);
      queue.notifyAll();
    }
  }

  public EventContext waitForAdd(long millis) throws InterruptedException {
    synchronized (queue) {
      if (queue.size() < 1) {
        queue.wait(millis);
      }
      return queue.size() < 1 ? null : (EventContext) queue.get(0);
    }
  }

  public EventContext take() throws InterruptedException {
    synchronized (queue) {
      while (queue.size() < 1) {
        queue.wait();
      }
      return (EventContext) queue.remove(0);
    }
  }

  public void setAddPredicate(AddPredicate predicate) {
    //
  }

  public AddPredicate getPredicate() {
    return null;
  }

  public int size() {
    return queue.size();
  }

  public List getInternalQueue() {
    return queue;
  }

  public void clear() {
    queue.clear();
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