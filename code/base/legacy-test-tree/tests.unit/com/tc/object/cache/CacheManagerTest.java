/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.cache;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

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
  SynchronizedInt          callCount        = new SynchronizedInt(0);

  Vector                   v                = new Vector();

  public void test() throws Exception {
    CacheManager cm = new CacheManager(this, new TestCacheConfig());
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
    for (int i = 1; i < 500000; i++) {
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
            + total + ". Pathway to OOME. Waiting for 5 sec or until reaping.");
        try {
          v.wait(5000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

  }

  public void evictCache(CacheStats stat) {
    synchronized (v) {
      int toEvict = stat.getObjectCountToEvict(v.size());
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
      stat.objectEvicted(evicted, v.size(), targetObjects4GC);
      if (callCount.increment() % 10 == 1) {
        log(stat.toString());
        log("Asked to evict - " + toEvict + " Evicted : " + evicted + " Vector Size : " + v.size());
      }
      v.notifyAll();
    }
  }

  Random r = new Random();

  private int rnd(int max) {
    return r.nextInt(max);
  }

  public class TestCacheConfig implements CacheConfig {

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

  }

}
