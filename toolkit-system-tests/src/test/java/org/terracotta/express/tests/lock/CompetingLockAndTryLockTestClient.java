/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;

public class CompetingLockAndTryLockTestClient extends ClientBase {

  private final CyclicBarrier      barrier = new CyclicBarrier(3);

  private final Map<Integer, Lock> locks   = new ConcurrentHashMap<Integer, Lock>();

  public CompetingLockAndTryLockTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) {
    Thread constructor = new Thread(new LockConstructor(toolkit), "Lock Constructor");
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
    private final Toolkit toolkit;

    public LockConstructor(Toolkit toolkit) {
      this.toolkit = toolkit;
    }

    public void run() {
      try {
        for (int i = 0; i < 100; i++) {
          locks.put(i, toolkit.getReadWriteLock("lock" + i).writeLock());
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
          l.lock();
          try {
            System.err.println("Locked " + i);
            barrier.await(); // 2
          } finally {
            l.unlock();
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
          if (l.tryLock()) {
            System.err.println("Try Locked " + i);
            l.unlock();
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

}
