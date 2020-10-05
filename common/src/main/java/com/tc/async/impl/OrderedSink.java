/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.async.impl;

import org.slf4j.Logger;

import com.tc.async.api.OrderedEventContext;
import com.tc.async.api.Sink;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class provides an order to the events processed. If events are added out of order, this class orderes them
 * before adding it to the destination sink. If Messages went missing, then this class waits till the missing message
 * arrives before pushing the events to the destination sink.
 * @param <T> Type the sink accepts
 */
public class OrderedSink<T extends OrderedEventContext> implements Sink<T> {

  private final Sink<T> sink;
  private final Logger logger;

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

  public OrderedSink(Logger logger, Sink<T> sink) {
    this.logger = logger;
    this.sink = sink;
  }

  @Override
  public synchronized void addToSink(T oc) {
    long seq = oc.getSequenceID();
    if (seq == 0) {
      if (pending.isEmpty()) {
        logger.debug("Sequence reset. Message with ID " + (current)
            + " was last before reset");
        current = 0;
        sink.addToSink(oc);
      } else {
        throw new AssertionError(pending.size() + " messages in pending queue. Message with ID " + (current + 1)
            + " is missing still but reset was requested");
      }
    } else if (seq <= current) {
      throw new AssertionError("Received Event with a sequence less than the current sequence. Current = " + current
          + " Seq Id = " + seq + " Event = " + oc);
    } else if (seq == current + 1) {
      current = seq;
      sink.addToSink(oc);
      processPendingIfNecessary();
    } else {
      pending.add(oc);
      if (pending.size() % 10 == 0) {
        logger.info(pending.size() + " messages in pending queue. Message with ID " + (current + 1)
            + " is missing still");
      }
    }
  }

  private void processPendingIfNecessary() {
    if (!pending.isEmpty()) {
      for (Iterator<T> i = pending.iterator(); i.hasNext();) {
        T oc = i.next();
        long seq = oc.getSequenceID();
        if (seq == current + 1) {
          current = seq;
          sink.addToSink(oc);
          i.remove();
        } else {
          break;
        }
      }
    }
  }
}
