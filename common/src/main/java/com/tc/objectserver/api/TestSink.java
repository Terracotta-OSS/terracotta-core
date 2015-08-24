/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.stats.Stats;

import java.util.LinkedList;
import java.util.List;

/**
 * @author steve
 */
public class TestSink<EC> implements Sink<EC> {
  private final List<EC> queue = new LinkedList<EC>();

  @Override
  public void addSingleThreaded(EC context) {
    synchronized (queue) {
      queue.add(context);
      queue.notifyAll();
    }
  }

  @Override
  public void addMultiThreaded(EC context) {
    // Not handled in this test.
    throw new UnsupportedOperationException();
    
  }
  
  @Override
  public void addSpecialized(SpecializedEventContext specialized) {
    // Not handled in this test.
    throw new UnsupportedOperationException();
    
  }

  public EC waitForAdd(long millis) throws InterruptedException {
    synchronized (queue) {
      if (queue.size() < 1) {
        queue.wait(millis);
      }
      return queue.size() < 1 ? null : (EC) queue.get(0);
    }
  }

  public EC take() throws InterruptedException {
    synchronized (queue) {
      while (queue.size() < 1) {
        queue.wait();
      }
      return queue.remove(0);
    }
  }

  @Override
  public int size() {
    return queue.size();
  }

  public List<EC> getInternalQueue() {
    return queue;
  }

  @Override
  public void clear() {
    queue.clear();
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
