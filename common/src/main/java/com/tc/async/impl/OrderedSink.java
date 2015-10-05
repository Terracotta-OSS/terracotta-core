/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.OrderedEventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.logging.TCLogger;
import com.tc.stats.Stats;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class provides an order to the events processed. If events are added out of order, this class orderes them
 * before adding it to the destination sink. If Messages went missing, then this class waits till the missing message
 * arrives before pushing the events to the destination sink.
 */
public class OrderedSink<T extends OrderedEventContext> implements Sink<T> {

  private final Sink<OrderedEventContext> sink;
  private final TCLogger logger;

  private long           current = 0;
  private final SortedSet<T> pending = new TreeSet<T>(new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        long s1 = o1.getSequenceID();
        long s2 = o2.getSequenceID();
        if (s1 < s2) return -1;
        else if (s1 == s2) return 0;
        else return 1;
      }
  });

  public OrderedSink(TCLogger logger, Sink<T> sink) {
    this.logger = logger;
    this.sink = (Sink<OrderedEventContext>)sink;
  }

  @Override
  public synchronized void addSingleThreaded(T oc) {
    long seq = oc.getSequenceID();
    if (seq <= current) {
      throw new AssertionError("Received Event with a sequence less than the current sequence. Current = " + current
          + " Seq Id = " + seq + " Event = " + oc);
    } else if (seq == current + 1) {
      current = seq;
      sink.addSingleThreaded(oc);
      processPendingIfNecessary();
    } else {
      pending.add(oc);
      if (pending.size() % 10 == 0) {
        logger.warn(pending.size() + " messages in pending queue. Message with ID " + (current + 1)
            + " is missing still");
      }
    }
  }

  @Override
  public void addMultiThreaded(OrderedEventContext specialized) {
    // Not used in the ordered case.
    throw new UnsupportedOperationException();
  }

  @Override
  public void addSpecialized(SpecializedEventContext specialized) {
    // Not used in the ordered case.
    throw new UnsupportedOperationException();
  }

  private void processPendingIfNecessary() {
    if (!pending.isEmpty()) {
      for (Iterator<T> i = pending.iterator(); i.hasNext();) {
        T oc = i.next();
        long seq = oc.getSequenceID();
        if (seq == current + 1) {
          current = seq;
          sink.addSingleThreaded(oc);
          i.remove();
        } else {
          break;
        }
      }
    }
  }

  @Override
  public synchronized void clear() {
    pending.clear();
    current = 0;
    sink.clear();
  }

  @Override
  public int size() {
    return sink.size();
  }

  @Override
  public void enableStatsCollection(boolean enable) {
    sink.enableStatsCollection(enable);
  }

  @Override
  public Stats getStats(long frequency) {
    return sink.getStats(frequency);
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    return sink.getStatsAndReset(frequency);
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    return sink.isStatsCollectionEnabled();
  }

  @Override
  public void resetStats() {
    sink.resetStats();
  }
}
