/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCTimerService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

public class ReInvalidateHandler {
  private final static long                  EXPIRE_SET_TIMER_PERIOD    = 60 * 1000;
  private final static long                  RE_INVALIDATE_TIMER_PERIOD = 1 * 1000;

  private final L1ServerMapLocalCacheManager l1ServerMapLocalCacheManager;
  private ConcurrentMapToObjectIDSet         prev                       = null;
  private ConcurrentMapToObjectIDSet         current                    = new ConcurrentMapToObjectIDSet();
  private final Timer                        timer                      = TCTimerService.getInstance()
                                                                            .getTimer("Re-invalidation Timer");

  public ReInvalidateHandler(L1ServerMapLocalCacheManager l1ServerMapLocalCacheManager) {
    this.l1ServerMapLocalCacheManager = l1ServerMapLocalCacheManager;
    timer.schedule(new ExpireSetTimerTask(), EXPIRE_SET_TIMER_PERIOD, EXPIRE_SET_TIMER_PERIOD);
    timer.schedule(new ReInvalidateTimerTask(), RE_INVALIDATE_TIMER_PERIOD, RE_INVALIDATE_TIMER_PERIOD);
  }

  public void add(ObjectID mapId, ObjectIDSet set) {
    current.add(mapId, set);
  }

  private class ExpireSetTimerTask extends TimerTask {
    @Override
    public void run() {
      synchronized (ReInvalidateHandler.this) {
        prev = current;
        current = new ConcurrentMapToObjectIDSet();
      }
    }
  }

  private class ReInvalidateTimerTask extends TimerTask {
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
          setFetched = new ObjectIDSet();
          map.put(mapId, setFetched);
        }

        setFetched.addAll(set);
      } finally {
        lock.unlock();
      }
    }
  }
}
