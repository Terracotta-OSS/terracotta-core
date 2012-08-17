/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import junit.framework.Assert;

public class TryLockTestClient extends ClientBase {

  public static final int NODE_COUNT = 2;

  public TryLockTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    final MockCoordinator coordinator = new MockCoordinator(toolkit);
    int id = getBarrierForAllClients().await();

    final MockQueue queue = new MockQueue("Queue[" + id + "]");

    try {
      // start both queues and let them compete for try locks
      coordinator.start(queue);

      Thread.sleep(2000);

      getBarrierForAllClients().await();

      // stop the first queue and wait for its thread to stop running
      if (id != 0) {
        coordinator.stop(queue);
        queue.getProcessingThread().join();
      }

      getBarrierForAllClients().await();

      // ensure that the succeeded try locks count on the second queue is zero
      // and let it run uncontended while getting tryLocks
      if (0 == id) {
        queue.resetTryLocksSucceededCount();
        Assert.assertEquals(0, queue.getTryLocksSucceededCount());

        Thread.sleep(2000);

        Assert.assertTrue(queue.getTryLocksSucceededCount() > 0);
      }

      getBarrierForAllClients().await();

      // again, ensure that the succeeded try locks count on the second queue is zero
      // explicitly take a lock and then let it run uncontended while getting tryLocks
      if (0 == id) {
        queue.resetTryLocksSucceededCount();
        Assert.assertEquals(0, queue.getTryLocksSucceededCount());

        coordinator.normalLock(queue);
        Thread.sleep(2000);

        Assert.assertTrue(queue.getTryLocksSucceededCount() > 0);

        coordinator.stop(queue);
      }

      getBarrierForAllClients().await();

      queue.getProcessingThread().join();

      getBarrierForAllClients().await();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      coordinator.stop(queue);
    }
  }

  public static class MockCoordinator {
    private final static String LOCK_ID = "coordinator_lock";
    private final Toolkit       toolkit;

    public MockCoordinator(Toolkit toolkit) {
      this.toolkit = toolkit;
    }

    public void start(final MockQueue queue) {
      System.out.println("> " + queue.getName() + " : start - lock()");
      ToolkitLock write = toolkit.getLock(LOCK_ID);
      write.lock();
      try {
        System.out.println("> " + queue.getName() + " : start - locked");
        queue.startProcessingThread(this);
      } finally {
        System.out.println("> " + queue.getName() + " : start - unlock()");
        write.unlock();
        System.out.println("> " + queue.getName() + " : start - unlocked");
      }
    }

    public void stop(final MockQueue queue) {
      synchronized (queue) {
        if (!queue.getProcessingThread().isAlive()) { return; }
      }

      System.out.println("> " + queue.getName() + " : stop - lock()");
      ToolkitLock write = toolkit.getLock(LOCK_ID);
      write.lock();
      try {
        System.out.println("> " + queue.getName() + " : stop - locked");
        queue.cancel();
      } finally {
        System.out.println("> " + queue.getName() + " : stop - unlock()");
        write.unlock();
        System.out.println("> " + queue.getName() + " : stop - unlocked");
      }
    }

    public void normalLock(final MockQueue queue) {
      System.out.println("> " + queue.getName() + " : normalLock - lock()");
      ToolkitLock write = toolkit.getLock(LOCK_ID);
      write.lock();
      try {
        System.out.println("> " + queue.getName() + " : normalLock - locked");
      } finally {
        System.out.println("> " + queue.getName() + " : normalLock - unlock()");
        write.unlock();
        System.out.println("> " + queue.getName() + " : normalLock - unlocked");
      }
    }

    boolean tryLock() {
      System.out.println("> " + Thread.currentThread().getName() + " : tryLock - tryLock()");
      ToolkitLock write = toolkit.getLock(LOCK_ID);

      if (write.tryLock()) {
        try {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        } finally {
          System.out.println("> " + Thread.currentThread().getName() + " : tryLock - unlock()");
          write.unlock();
          System.out.println("> " + Thread.currentThread().getName() + " : tryLock - unlocked");
        }
        return true;
      }

      return false;
    }
  }

  public static class MockQueue {
    private transient final String name;
    private transient Thread       processingThread;

    private MockCoordinator        coordinator;
    private boolean                cancelled         = false;
    private int                    tryLocksSucceeded = 0;

    public MockQueue(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    synchronized void startProcessingThread(final MockCoordinator processingCoordinator) {
      coordinator = processingCoordinator;
      processingThread = new Thread(new ProcessingThread(), name);
      processingThread.setDaemon(true);
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

    public synchronized void resetTryLocksSucceededCount() {
      tryLocksSucceeded = 0;
    }

    public synchronized int getTryLocksSucceededCount() {
      return tryLocksSucceeded;
    }

    private void processItems() {
      if (coordinator.tryLock()) {
        synchronized (this) {
          tryLocksSucceeded++;
        }
      }
    }
  }
}
