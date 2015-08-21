package com.tc.async.impl;

import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.stats.Stats;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author orion
 */
public class MockSink<EC> implements Sink<EC> {

  public BlockingQueue<EC> queue = new LinkedBlockingQueue<>(); // its not bounded

  public EC take() {
    try {
      return this.queue.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void addSingleThreaded(EC context) {
    try {
      this.queue.put(context);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void addMultiThreaded(EC context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addSpecialized(SpecializedEventContext specialized) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return this.queue.size();
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
