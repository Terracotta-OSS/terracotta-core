/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.object.locks;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.builtin.ConcurrentHashMap;
import com.tctest.builtin.Lock;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CompetingLockAndTryLockTestApp extends AbstractTransparentApp {

  private final CyclicBarrier      barrier = new CyclicBarrier(3);

  private final Map<Integer, Lock> locks   = new ConcurrentHashMap<Integer, Lock>();

  public CompetingLockAndTryLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    Thread constructor = new Thread(new LockConstructor(), "Lock Constructor");
    Thread locker = new Thread(new Locker(), "Locker");
    Thread tryLocker = new Thread(new TryLocker(), "TryLocker");

    constructor.start();
    locker.start();
    tryLocker.start();

    try {
      constructor.join();
      locker.join();
      tryLocker.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  class LockConstructor implements Runnable {
    public void run() {
      try {
        for (int i = 0; i < 100; i++) {
          locks.put(i, new Lock());
          System.err.println("Constructed Lock " + i);
          barrier.await(); // 1
          barrier.await(); // 2
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (BrokenBarrierException e) {
        e.printStackTrace();
      }
    }
  }

  class Locker implements Runnable {
    public void run() {
      try {
        for (int i = 0; i < 100; i++) {
          barrier.await(); // 1
          final Lock l = locks.get(i);
          l.writeLock();
          try {
            System.err.println("Locked " + i);
            barrier.await(); // 2
          } finally {
            l.writeUnlock();
            System.err.println("Unlocked " + i);
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (BrokenBarrierException e) {
        e.printStackTrace();
      }
    }
  }

  class TryLocker implements Runnable {
    public void run() {
      try {
        for (int i = 0; i < 100; i++) {
          barrier.await(); // 1
          final Lock l = locks.get(i);
          if (l.tryWriteLock()) {
            System.err.println("Try Locked " + i);
            l.writeUnlock();
            System.err.println("Try Unlocked " + i);
          } else {
            System.err.println("Failed Try Lock " + i);
          }
          barrier.await(); // 2
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (BrokenBarrierException e) {
        e.printStackTrace();
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = CompetingLockAndTryLockTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    spec.addRoot("locks", "locks");
  }
}
