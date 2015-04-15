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

  @Override
  public boolean addLossy(EventContext context) {
    return false;
  }

  @Override
  public void addMany(Collection contexts) {
    //
  }

  @Override
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

  @Override
  public void setAddPredicate(AddPredicate predicate) {
    //
  }

  @Override
  public AddPredicate getPredicate() {
    return null;
  }

  @Override
  public int size() {
    return queue.size();
  }

  public List getInternalQueue() {
    return queue;
  }

  @Override
  public void clear() {
    queue.clear();
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
