/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.test.TCTestCase;
import com.tc.util.DebugUtil;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

public class CacheManagerTest extends TCTestCase implements Evictable {

  int             usedT            = 50;
  int             usedCritialT     = 90;
  long            sleepInterval    = 500;
  int             lc               = 2;
  int             percentage2Evict = 10;
  SynchronizedInt callCount        = new SynchronizedInt(0);

  Vector          v                = new Vector();

  // XXX:: In solaris this test seems to fail a lot. trying out different sleep time to see the effect on it.
  boolean         isSolaris        = Os.isSolaris();

  public void test() throws Exception {
    CacheManager cm = new CacheManager(this, new TestCacheConfig());
    log("Cache Manager Created : " + cm);
    hogMemory();
    assertTrue(callCount.get() > 0);
  }

  private void log(String message) {
    System.err.println(time() +" - " + thread() + " : " + message);
  }

  private String time() {
   return new Date().toString(); 
  }

  private String thread() {
    return Thread.currentThread().getName();
  }

  private void hogMemory() {
    for (int i = 1; i < 500000; i++) {
      byte[] b = new byte[10240];
      v.add(b);
      int size;
      if (i == 100) {
        // Sometimes in some platforms the memory manager thread is not given any time to start up, so
        ThreadUtil.reallySleep(2000);
      } else if (i % 10000 == 0) {
        log("Created " + i + " byte arrays - currently in vector = " + v.size());
        ThreadUtil.reallySleep(500);
      } else if (i % 50 == 0) {
        if (isSolaris) {
          ThreadUtil.reallySleep(1);
        } else {
          ThreadUtil.reallySleep(0, 10);
        }
      } else if (isSolaris && (size = v.size()) >= 5000 && size % 50 == 0) {
        log("WARNING :: Vector Size reached " + size + ". Pathway to OOME when running with 64MB heap. Waiting till reaping.");
        waitTillNotified();
      }
    }
  }

  private void waitTillNotified() {
    synchronized(v) {
      //XXX::Explicitly not doing a spin lock
      if(v.size() >= 5000) {
        DebugUtil.DEBUG =true;
        try {
          v.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        DebugUtil.DEBUG =false;
      }
    }
    
  }

  public void evictCache(CacheStats stat) {
    if(isSolaris) {
      log("SOlaris : " + stat);
    }
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
      if (callCount.increment() % 10 == 1 || isSolaris) {
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
