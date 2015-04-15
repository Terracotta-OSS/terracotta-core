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
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.stats.Stats;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author orion
 */
public class MockSink implements Sink {

  public BlockingQueue<EventContext> queue = new LinkedBlockingQueue<EventContext>(); // its not bounded

  public EventContext take() {
    try {
      return this.queue.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  @Override
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

  @Override
  public void addMany(Collection contexts) {
    for (Iterator i = contexts.iterator(); i.hasNext();)
      try {
        this.queue.put((EventContext) i.next());
      } catch (Exception e) {
        throw new AssertionError(e);
      }
  }

  @Override
  public void add(EventContext context) {
    try {
      this.queue.put(context);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void setAddPredicate(AddPredicate predicate) {
    throw new ImplementMe();
  }

  @Override
  public AddPredicate getPredicate() {
    throw new ImplementMe();
  }

  @Override
  public int size() {
    return this.queue.size();
  }

  public void turnTracingOn() {
    throw new ImplementMe();
  }

  public void turnTracingOff() {
    throw new ImplementMe();
  }

  @Override
  public void clear() {
    throw new ImplementMe();

  }

  public void pause(List pauseEvents) {
    throw new ImplementMe();

  }

  public void unpause() {
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