/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockLockInterruptiblyTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;
  private static final int JOIN_TIMEOUT = 5 * 60 * 1000;
    
  public ReentrantReadWriteLockLockInterruptiblyTest() {
    //
  }

  protected Class getApplicationClass() {
    return ReentrantReadWriteLockLockInterruptiblyTest.App.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  public static class App extends AbstractTransparentApp {

    private final ReentrantReadWriteLock subject = new ReentrantReadWriteLock();
    private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());
    private final FailureToggle status = new FailureToggle();
    private final Random rndm = new Random();
    private final Map<Thread, Throwable> uncaughtExceptions = new ConcurrentHashMap();
    private final UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
      public void uncaughtException(Thread t, Throwable e) {
        uncaughtExceptions.put(t, e);
      }      
    };

    public App(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
      super(appId, config, listenerProvider);
    }

    public void run() {
      try {
        if (!testLockInterrupt(subject.writeLock(), subject.writeLock(), barrier.await())) return;
        if (!testLockInterrupt(subject.writeLock(), subject.readLock(), barrier.await())) return;
        if (!testLockInterrupt(subject.readLock(), subject.writeLock(), barrier.await())) return;

        if (!testLockInterruptiblyBeforeAward(subject.writeLock(), subject.writeLock(), barrier.await())) return;
        if (!testLockInterruptiblyBeforeAward(subject.writeLock(), subject.readLock(), barrier.await()))return;
        if (!testLockInterruptiblyBeforeAward(subject.readLock(), subject.writeLock(), barrier.await())) return;
        
        if (!testRepeatedLockAcquire(subject.readLock(), barrier.await())) return;
      } catch (BrokenBarrierException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private boolean testLockInterrupt(final Lock first, final Lock second, int index) throws BrokenBarrierException, InterruptedException {
      if (index == 0) {
        System.err.println("Testing lockInterruptibly interruption on locking " + second.getClass().getSimpleName() + " while " + first.getClass().getSimpleName() + " is held");
        first.lock();
      }
      try {
        barrier.await();

        if (index != 0) {
          Thread t = new Thread() {
            public void run() {
              while (true) {
                try {
                  second.lockInterruptibly();
                } catch (InterruptedException e) {
                  return;
                }
              }
            }
          };
          t.setUncaughtExceptionHandler(handler);
          t.start();

          ThreadUtil.reallySleep(rndm.nextInt(2000));

          t.interrupt();

          t.join(JOIN_TIMEOUT);

          if (t.isAlive()) {
            status.failed();
            barrier.reset();
            Assert.fail("Interrupting failed to stop thread waiting to lock " + second.getClass().getSimpleName());
          }
        }

        return waitForCompletion(barrier, status);
      } finally {
        if (index == 0) {
          first.unlock();
        }        
      }
    }

    private boolean testLockInterruptiblyBeforeAward(final Lock first, final Lock second, int index) throws BrokenBarrierException, InterruptedException {
      if (index == 0) {
        System.err.println("Testing lockInterruptibly acquire on " + second.getClass().getSimpleName() + " after releasing " + first.getClass().getSimpleName());
        first.lock();
      }

      barrier.await();

      Thread t = null;
      if (index != 0) {
        t = new Thread() {
          public void run() {
            while (true) {
              try {
                second.lockInterruptibly();
              } catch (InterruptedException e) {
                continue;
              }
              break;
            }

            second.unlock();
          }
        };

        t.start();
      }

      barrier.await();

      ThreadUtil.reallySleep(1000);

      if (index == 0) {
        first.unlock();
      } else {
        t.join(JOIN_TIMEOUT);
        if (t.isAlive()) {
          status.failed(); 
          barrier.reset();
          Assert.fail("Failed to acquire lock " + second.getClass().getSimpleName() + " using lockInterruptibly");
        }
      }

      return waitForCompletion(barrier, status);
    }

    private boolean testRepeatedLockAcquire(final Lock lock, int index) throws BrokenBarrierException, InterruptedException {
      if (index == 0) {
        System.err.println("Testing multiple lockInterruptibly acquires on " + lock.getClass().getSimpleName());
      }

      barrier.await();

      Thread t = new Thread() {
        public void run() {
          try {
            lock.lockInterruptibly();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          
          try {
            barrier.await();
          } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          
          lock.unlock();
        }
      };
      t.setUncaughtExceptionHandler(handler);
      t.start();

      t.join(JOIN_TIMEOUT);

      if (t.isAlive()) {
        status.failed();
        barrier.reset();
        Assert.fail("Failed to acquire lock " + lock.getClass().getSimpleName() + " using lockInterruptibly");
      } else if (uncaughtExceptions.containsKey(t)) {
        status.failed();
        barrier.reset();
        throw Assert.failure("Locking thread terminated abnormally", uncaughtExceptions.get(t));                     
      }
      
      return waitForCompletion(barrier, status);      
    }
    
    private static final boolean waitForCompletion(CyclicBarrier barrier, FailureToggle status) throws InterruptedException {
      try {
        barrier.await();
      } catch (BrokenBarrierException e) {
        //
      }
      return !status.hasFailed();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = ReentrantReadWriteLockLockInterruptiblyTest.App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("subject", "subject");
      spec.addRoot("barrier", "barrier");
      spec.addRoot("status", "status");

      config.addWriteAutolock("* " + testClass + "*.*(..)");

      config.getOrCreateSpec(FailureToggle.class.getName());
      config.addWriteAutolock("* " + FailureToggle.class.getName() + "*.*(..)");

      new CyclicBarrierSpec().visit(visitor, config);
    }

    private static class FailureToggle {
      private boolean failed = false;

      public synchronized boolean hasFailed() {
        return failed;
      }

      public synchronized void failed() {
        failed = true;
      }
    }
  }
}
