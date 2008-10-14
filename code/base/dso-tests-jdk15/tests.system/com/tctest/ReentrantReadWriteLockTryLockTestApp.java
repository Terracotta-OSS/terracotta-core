/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import junit.framework.Assert;

public class ReentrantReadWriteLockTryLockTestApp extends AbstractTransparentApp {

  public static final int NODE_COUNT = 2;
  
  private final CyclicBarrier barrier = new CyclicBarrier(NODE_COUNT);
  private final MockCoordinator coordinator = new MockCoordinator();

  public ReentrantReadWriteLockTryLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    int id;
    try {
      id = barrier.await();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    final MockQueue queue = new MockQueue(getApplicationId());
    
    try {
      // start both queues and let them compete for try locks
      coordinator.start(queue);

      Thread.sleep(2000);

      barrier.await();

      // stop the first queue and wait for its thread to stop running
      if (id != 0) {
        coordinator.stop(queue);
        queue.getProcessingThread().join();
      }

      barrier.await();

      // ensure that the failed try locks count on the second queue is zero
      // and let it run uncontended while getting tryLocks
      if (0 == id) {
        queue.resetTryLocksFailCount();
        Assert.assertEquals(0, queue.getTryLocksFailCount());
        
        Thread.sleep(2000);

        Assert.assertEquals(0, queue.getTryLocksFailCount());
      }
      
      barrier.await();

      // again, ensure that the failed try locks count on the second queue is zero
      // explicitly take a lock and then let it run uncontended while getting tryLocks
      if (0 == id) {
        queue.resetTryLocksFailCount();
        Assert.assertEquals(0, queue.getTryLocksFailCount());

        coordinator.normalLock(queue);
        Thread.sleep(2000);

        Assert.assertEquals(0, queue.getTryLocksFailCount());

        coordinator.stop(queue);
      }
      
      barrier.await();
      
      queue.getProcessingThread().join();

      barrier.await();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      coordinator.stop(queue);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ReentrantReadWriteLockTryLockTestApp.class.getName();
    config
      .getOrCreateSpec(testClass)
      .addRoot("barrier", "barrier")
      .addRoot("coordinator", "coordinator");
    config.addWriteAutolock("* " + testClass + "*.*(..)");
    
    config.getOrCreateSpec(MockCoordinator.class.getName());
    config.addWriteAutolock("* " + MockCoordinator.class.getName() + "*.*(..)");
    config.getOrCreateSpec(MockQueue.class.getName());
    config.addWriteAutolock("* " + MockQueue.class.getName() + "*.*(..)");

    new CyclicBarrierSpec().visit(visitor, config);
  }
  
  public static class MockCoordinator {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public void start(final MockQueue queue) {
      System.out.println("> "+queue.getName()+" : start - lock()");
      lock.writeLock().lock();
      try {
        System.out.println("> "+queue.getName()+" : start - locked");
        queue.startProcessingThread(this);
      } finally {
        System.out.println("> "+queue.getName()+" : start - unlock()");
        lock.writeLock().unlock();
        System.out.println("> "+queue.getName()+" : start - unlocked");
      }
    }
    
    public void stop(final MockQueue queue) {
      System.out.println("> "+queue.getName()+" : stop - lock()");
      lock.writeLock().lock();
      try {
        System.out.println("> "+queue.getName()+" : stop - locked");
        queue.cancel();
      } finally {
        System.out.println("> "+queue.getName()+" : stop - unlock()");
        lock.writeLock().unlock();
        System.out.println("> "+queue.getName()+" : stop - unlocked");
      }
    }
    
    public void normalLock(final MockQueue queue) {
      System.out.println("> "+queue.getName()+" : normalLock - lock()");
      lock.writeLock().lock();
      try {
        System.out.println("> "+queue.getName()+" : normalLock - locked");
      } finally {
        System.out.println("> "+queue.getName()+" : normalLock - unlock()");
        lock.writeLock().unlock();
        System.out.println("> "+queue.getName()+" : normalLock - unlocked");
      }
    }

    boolean tryLock() {
      return tryLockWithoutTimeout();
      // this test works when there's a timeout
//      return tryLockWithTimeout();
    }

    boolean tryLockWithoutTimeout() {
      System.out.println("> "+Thread.currentThread().getName()+" : tryLock - tryLock()");
      if (lock.readLock().tryLock()) {
        try {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        } finally {
          System.out.println("> "+Thread.currentThread().getName()+" : tryLock - unlock()");
          lock.readLock().unlock();
          System.out.println("> "+Thread.currentThread().getName()+" : tryLock - unlocked");
        }
        return true;
      }
      
      return false;
    }

    boolean tryLockWithTimeout() {
      System.out.println("> "+Thread.currentThread().getName()+" : tryLock - tryLock()");
      try {
        if (lock.readLock().tryLock(1, TimeUnit.MICROSECONDS)) {
          try {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          } finally {
            System.out.println("> "+Thread.currentThread().getName()+" : tryLock - unlock()");
            lock.readLock().unlock();
            System.out.println("> "+Thread.currentThread().getName()+" : tryLock - unlocked");
          }
          return true;
        }
        
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      
      return false;
    }
  }

  public static class MockQueue {
    private transient Thread  processingThread;
    private transient String  name;
    private MockCoordinator   coordinator;
    private boolean           cancelled = false;
    private int               tryLocksFailed = 0;
    
    public MockQueue(final String name) {
      this.name = name;
    }
    
    public String getName() {
      return name;
    }

    synchronized void startProcessingThread(final MockCoordinator processingCoordinator) {
      coordinator = processingCoordinator;
      processingThread = new Thread(new ProcessingThread(), name);
      processingThread.start();
    }
    
    public Thread getProcessingThread() {
      return processingThread;
    }
    
    private final class ProcessingThread implements Runnable {
      public void run() {

        while (!isCancelled()) {
          processItems();

          try {
            synchronized (MockQueue.this) {
              MockQueue.this.wait(100);
            }
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }

      private boolean isCancelled() {
        synchronized (MockQueue.this) {
          return cancelled;
        }
      }
    }
    
    public synchronized void cancel() {
      cancelled = true;
      MockQueue.this.notifyAll();      
    }
    
    public synchronized void resetTryLocksFailCount() {
      tryLocksFailed = 0;
    }
    
    public synchronized int getTryLocksFailCount() {
      return tryLocksFailed;
    }
    
    private void processItems() {
      if (!coordinator.tryLock()) {
        synchronized (this) {
          tryLocksFailed++;
        }
      }
    }
  }
}