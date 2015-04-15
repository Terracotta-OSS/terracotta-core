/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.async.impl;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.OrderedEventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.stats.Stats;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class provides an order to the events processed. If events are added out of order, this class orderes them
 * before adding it to the destination sink. If Messages went missing, then this class waits till the missing message
 * arrives before pushing the events to the destination sink.
 */
public class OrderedSink implements Sink {

  private final Sink     sink;
  private final TCLogger logger;

  private long           current = 0;
  private SortedSet      pending = new TreeSet(new Comparator() {
                                   @Override
                                  public int compare(Object o1, Object o2) {
                                     long s1 = ((OrderedEventContext) o1).getSequenceID();
                                     long s2 = ((OrderedEventContext) o2).getSequenceID();
                                     if (s1 < s2) return -1;
                                     else if (s1 == s2) return 0;
                                     else return 1;
                                   }
                                 });
  private AddPredicate   predicate;

  public OrderedSink(TCLogger logger, Sink sink) {
    this.logger = logger;
    this.sink = sink;
    this.predicate = DefaultAddPredicate.getInstance();
  }

  @Override
  public synchronized void add(EventContext context) {
    if (!predicate.accept(context)) {
      logger.warn("Predicate forced to ignore message " + context);
      return;
    }
    OrderedEventContext oc = (OrderedEventContext) context;
    long seq = oc.getSequenceID();
    if (seq <= current) {
      throw new AssertionError("Received Event with a sequence less than the current sequence. Current = " + current
          + " Seq Id = " + seq + " Event = " + oc);
    } else if (seq == current + 1) {
      current = seq;
      sink.add(context);
      processPendingIfNecessary();
    } else {
      pending.add(oc);
      if (pending.size() % 10 == 0) {
        logger.warn(pending.size() + " messages in pending queue. Message with ID " + (current + 1)
            + " is missing still");
      }
    }
  }

  private void processPendingIfNecessary() {
    if (!pending.isEmpty()) {
      for (Iterator i = pending.iterator(); i.hasNext();) {
        OrderedEventContext oc = (OrderedEventContext) i.next();
        long seq = oc.getSequenceID();
        if (seq == current + 1) {
          current = seq;
          sink.add(oc);
          i.remove();
        } else {
          break;
        }
      }
    }
  }

  /**
   * this implementation isnt really lossy.
   */
  @Override
  public boolean addLossy(EventContext context) {
    add(context);
    return true;
  }

  @Override
  public void addMany(Collection contexts) {
    for (Iterator i = contexts.iterator(); i.hasNext();) {
      EventContext ec = (EventContext) i.next();
      add(ec);
    }
  }

  @Override
  public synchronized void clear() {
    pending.clear();
    current = 0;
    sink.clear();
  }

  @Override
  public synchronized AddPredicate getPredicate() {
    return predicate;
  }

  @Override
  public synchronized void setAddPredicate(AddPredicate ap) {
    this.predicate = ap;
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
