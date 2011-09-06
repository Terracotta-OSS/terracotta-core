/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.cache;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.statistics.mock.NullStatisticsAgentSubSystem;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

public class CacheManagerTest extends TCTestCase implements Evictable {

  private static final int BYTES_SIZE       = 10240;
  int                      usedT            = 50;
  int                      usedCritialT     = 90;
  long                     sleepInterval    = 500;
  int                      lc               = 2;
  int                      percentage2Evict = 10;
  SynchronizedInt          callCount;

  Vector                   v;
  TestCacheConfig          cacheConfig;
  TCMemoryManagerImpl      tcMemManager;
  TCThreadGroup            thrdGrp;

  @Override
  public void setUp() {
    callCount = new SynchronizedInt(0);
    v = new Vector();
    cacheConfig = new TestCacheConfig();
    thrdGrp = new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(CacheManagerTest.class)));
    tcMemManager = new TCMemoryManagerImpl(cacheConfig.getSleepInterval(), cacheConfig.getLeastCount(), cacheConfig
        .isOnlyOldGenMonitored(), thrdGrp, true);
    System.gc();
    System.gc();
    System.gc();
  }

  public void test() throws Exception {
    CacheManager cm = new CacheManager(this, cacheConfig, thrdGrp, new NullStatisticsAgentSubSystem(), tcMemManager);
    cm.start();
    log("Cache Manager Created : " + cm);
    hogMemory();
    assertTrue(callCount.get() > 0);
  }

  public void testCritcalObjectCount() throws Exception {
    cacheConfig.criticalObjectCount = 500;
    CacheManager cm = new CacheManager(new CritialObjectCountCacheValidator(cacheConfig
        .getObjectCountCriticalThreshold(), cacheConfig.getPercentageToEvict()), cacheConfig, thrdGrp,
                                       new NullStatisticsAgentSubSystem(), tcMemManager);
    cm.start();
    log("Cache Manager Created : " + cm);
    hogMemory();
    assertTrue(callCount.get() > 0);
  }

  private void log(String message) {
    System.err.println(time() + " - " + thread() + " : " + message);
  }

  private String time() {
    return new Date().toString();
  }

  private String thread() {
    return Thread.currentThread().getName();
  }

  private void hogMemory() {
    for (int i = 1; i < 300000; i++) {
      byte[] b = new byte[BYTES_SIZE];
      v.add(b);
      if (i == 100) {
        // Sometimes in some platforms the memory manager thread is not given any time to start up, so
        ThreadUtil.reallySleep(2000);
      } else if (i % 10000 == 0) {
        log("Created " + i + " byte arrays - currently in vector = " + v.size());
        ThreadUtil.reallySleep(500);
      } else if (i % 50 == 0) {
        ThreadUtil.reallySleep(1);
      } else if (i % 100 == 1) {
        waitTillNotifiedIfCritical();
      }
    }
  }

  private void waitTillNotifiedIfCritical() {
    synchronized (v) {
      Runtime runtime = Runtime.getRuntime();
      long max = runtime.maxMemory();
      long total = runtime.totalMemory();
      long free = runtime.freeMemory();
      // XXX::Explicitly not doing a spin lock
      if (total >= max * 0.97 && free < max * 0.05) {
        // free memory is less than 5 % of max memory
        log("WARNING :: Vector Size reached " + v.size() + " and free = " + free + " max = " + max + " total = "
            + total + ". Pathway to OOME. Waiting for 5O msec or until reaping.");
        try {
          v.wait(50);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

  }

  public void evictCache(CacheStats stat) {
    synchronized (v) {
      int toEvict = stat.getObjectCountToEvict(v.size());
      evict(stat, toEvict);
    }
  }

  private void evict(CacheStats stat, int toEvict) {
    int evicted = 0;
    ArrayList targetObjects4GC = new ArrayList();
    if (toEvict >= v.size()) {
      evicted = v.size();
      targetObjects4GC.addAll(v);
      v.clear();
    } else {
      for (int i = 0; i < toEvict; i++) {
        targetObjects4GC.add(v.remove(rnd(v.size())));
        evicted++;
      }
    }
    stat.objectEvicted(evicted, v.size(), targetObjects4GC, true);
    if (callCount.increment() % 10 == 1) {
      log(stat.toString());
      log("Asked to evict - " + toEvict + " Evicted : " + evicted + " Vector Size : " + v.size());
    }
    v.notifyAll();
  }

  Random r = new Random();

  private int rnd(int max) {
    return r.nextInt(max);
  }

  public class CritialObjectCountCacheValidator implements Evictable {

    private final int criticalObjectCount;
    private final int evictionPercentage;

    public CritialObjectCountCacheValidator(int criticalObjectCount, int evictionPercentage) {
      this.criticalObjectCount = criticalObjectCount;
      this.evictionPercentage = evictionPercentage;
    }

    public void evictCache(CacheStats stat) {
      synchronized (v) {
        int toEvict = stat.getObjectCountToEvict(v.size());
        int willRemain = v.size() - toEvict;
        log("Current Size : " + v.size() + " To Evict : " + toEvict + " Will Remain : " + willRemain);
        if (toEvict == 0) return; // Below threshold
        assertTrue(willRemain <= criticalObjectCount);
        assertTrue((willRemain + (2 * criticalObjectCount * evictionPercentage / 100)) > criticalObjectCount);
        CacheManagerTest.this.evict(stat, toEvict);
      }
    }
  }

  public class TestCacheConfig implements CacheConfig {

    int criticalObjectCount = -1;

    public int getUsedCriticalThreshold() {
      return usedCritialT;
    }

    public int getUsedThreshold() {
      return usedT;
    }

    public int getLeastCount() {
      return lc;
    }

    public int getPercentageToEvict() {
      return percentage2Evict;
    }

    public long getSleepInterval() {
      return sleepInterval;
    }

    public boolean isOnlyOldGenMonitored() {
      return true;
    }

    public boolean isLoggingEnabled() {
      return false;
    }

    public int getObjectCountCriticalThreshold() {
      return criticalObjectCount;
    }

  }

}
