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
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReInvalidateHandler {
  private final static long                  EXPIRE_SET_TIMER_PERIOD    = 60 * 1000;
  private final static long                  RE_INVALIDATE_TIMER_PERIOD = 1 * 1000;

  private final L1ServerMapLocalCacheManager l1ServerMapLocalCacheManager;
  private ConcurrentMapToObjectIDSet         prev;
  private ConcurrentMapToObjectIDSet         current                    = new ConcurrentMapToObjectIDSet();
  private final Timer                        timer;

  public ReInvalidateHandler(final L1ServerMapLocalCacheManager l1ServerMapLocalCacheManager,
                             final TaskRunner taskRunner) {
    this.l1ServerMapLocalCacheManager = l1ServerMapLocalCacheManager;
    this.timer = taskRunner.newTimer("Re-invalidation Timer");
    this.timer.scheduleWithFixedDelay(new ExpireSetTimerTask(),
        EXPIRE_SET_TIMER_PERIOD, EXPIRE_SET_TIMER_PERIOD, TimeUnit.MILLISECONDS);
    this.timer.scheduleWithFixedDelay(new ReInvalidateTimerTask(),
        RE_INVALIDATE_TIMER_PERIOD, RE_INVALIDATE_TIMER_PERIOD, TimeUnit.MILLISECONDS);
  }

  public void shutdown() {
    timer.cancel();
  }

  public synchronized void add(ObjectID mapId, ObjectIDSet set) {
    current.add(mapId, set);
  }

  private class ExpireSetTimerTask implements Runnable {
    @Override
    public void run() {
      synchronized (ReInvalidateHandler.this) {
        prev = current;
        current = new ConcurrentMapToObjectIDSet();
      }
    }
  }

  private class ReInvalidateTimerTask implements Runnable {
    @Override
    public void run() {
      final ConcurrentMapToObjectIDSet tempPrev;
      final ConcurrentMapToObjectIDSet tempCurrent;

      synchronized (ReInvalidateHandler.this) {
        tempPrev = prev;
        tempCurrent = current;
      }

      if (tempPrev != null) {
        tempPrev.processInvalidations();
      }

      if (tempCurrent != null) {
        tempCurrent.processInvalidations();
      }
    }
  }

  private class ConcurrentMapToObjectIDSet {
    @Override
    public String toString() {
      return "ConcurrentMapToObjectIDSet [maps=" + Arrays.toString(maps) + "]";
    }

    private static final int                   CONCURRENCY = 4;

    private final ReentrantLock[]              locks;
    private final Map<ObjectID, ObjectIDSet>[] maps;

    public ConcurrentMapToObjectIDSet() {
      this(CONCURRENCY);
    }

    public ConcurrentMapToObjectIDSet(int concurrency) {
      locks = new ReentrantLock[concurrency];
      maps = new Map[concurrency];
      for (int i = 0; i < concurrency; i++) {
        maps[i] = new HashMap<ObjectID, ObjectIDSet>();
        locks[i] = new ReentrantLock();
      }
    }

    private ReentrantLock getLock(ObjectID oid) {
      return locks[(int) (Math.abs(oid.toLong()) % CONCURRENCY)];
    }

    private Map<ObjectID, ObjectIDSet> getMap(ObjectID oid) {
      return maps[(int) (Math.abs(oid.toLong()) % CONCURRENCY)];
    }

    public void processInvalidations() {
      for (int i = 0; i < maps.length; i++) {
        ReentrantLock lock = locks[i];
        Map<ObjectID, ObjectIDSet> map = maps[i];

        lock.lock();
        try {
          if (map.size() == 0) {
            continue;
          }

          Iterator<Entry<ObjectID, ObjectIDSet>> iterator = map.entrySet().iterator();
          while (iterator.hasNext()) {
            Entry<ObjectID, ObjectIDSet> entry = iterator.next();
            ObjectID mapId = entry.getKey();
            ObjectIDSet oidSet = entry.getValue();

            ObjectIDSet setNow = l1ServerMapLocalCacheManager.removeEntriesForObjectId(mapId, oidSet);

            if (setNow.isEmpty()) {
              iterator.remove();
            } else {
              oidSet.retainAll(setNow);
            }
          }
        } finally {
          lock.unlock();
        }
      }
    }

    public void add(ObjectID mapId, ObjectIDSet set) {
      ReentrantLock lock = getLock(mapId);
      Map<ObjectID, ObjectIDSet> map = getMap(mapId);
      lock.lock();

      try {
        ObjectIDSet setFetched = map.get(mapId);
        if (setFetched == null) {
          setFetched = new BitSetObjectIDSet();
          map.put(mapId, setFetched);
        }

        setFetched.addAll(set);
      } finally {
        lock.unlock();
      }
    }
  }
}
