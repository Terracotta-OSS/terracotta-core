/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCObjectNotSharableException;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ReentrantReadWriteLockTestApp extends AbstractTransparentApp {
  public final static String  CRASH_TEST        = "CRASH_TEST";
  private final static int    NUM_OF_PUTS       = 400;
  private final static int    IBM_NUM_OF_PUTS   = 300;
  private final int           putsCount;

  private final CyclicBarrier barrier, barrier2, barrier3, barrier4, barrier5;
  private final DataRoot      dataRoot          = new DataRoot();

  private final List          queue             = new LinkedList();
  private final List          shareableLockList = new ArrayList();
  private final Random        random;

  private final int           numOfPutters      = 1;
  private final int           numOfGetters;
  private final boolean       isCrashTest;

  public ReentrantReadWriteLockTestApp(final String appId, final ApplicationConfig cfg,
                                       final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    barrier2 = new CyclicBarrier(getParticipantCount());
    barrier3 = new CyclicBarrier(getParticipantCount());
    barrier4 = new CyclicBarrier(2);
    barrier5 = new CyclicBarrier(3);
    if (Vm.isIBM()) {
      putsCount = IBM_NUM_OF_PUTS;
    } else {
      putsCount = NUM_OF_PUTS;
    }

    random = new Random(new Random(System.currentTimeMillis() + getApplicationId().hashCode()).nextLong());
    numOfGetters = getParticipantCount() - numOfPutters;

    isCrashTest = "true".equals(cfg.getAttribute(CRASH_TEST)) ? true : false;
  }

  public void run() {
    try {
      int index = barrier.await();
      barrier.await();

      unsharedToSharedTest(index, new ReentrantReadWriteLock());

      unsharedToSharedReadLockTest(index, new ReentrantReadWriteLock());

      unsharedToSharedWriteLockTest(index, new ReentrantReadWriteLock());

      readWriteLockTest(index, false);
      ReentrantReadWriteLock localLock = new ReentrantReadWriteLock();
      singleNodeConditionVariableTesting(index, localLock.writeLock(), localLock.writeLock().newCondition());

      readWriteLockTest(index, true);
      localLock = new ReentrantReadWriteLock(true);
      singleNodeConditionVariableTesting(index, localLock.writeLock(), localLock.writeLock().newCondition());

      barrier.await();

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void unsharedToSharedReadLockTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    if (index == 0) {
      ReadLock readLock = lock.readLock();
      readLock.lock();
      try {
        synchronized (shareableLockList) {
          shareableLockList.add(readLock);
        }
        throw new AssertionError("Should have expected a TCObjectNotSharableException.");
      } catch (TCObjectNotSharableException e) {
        // expected
      } finally {
        readLock.unlock();
      }
      Assert.assertFalse(ManagerUtil.isManaged(lock));
    }

    barrier.await();
  }

  private void unsharedToSharedWriteLockTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    if (index == 0) {
      WriteLock writeLock = lock.writeLock();
      writeLock.lock();
      try {
        synchronized (shareableLockList) {
          shareableLockList.add(writeLock);
        }
        throw new AssertionError("Should have expected a TCObjectNotSharableException.");
      } catch (TCObjectNotSharableException e) {
        // expected
      } finally {
        writeLock.unlock();
      }
      Assert.assertFalse(ManagerUtil.isManaged(lock));
    }

    barrier.await();
  }

  private void tryLockTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "tryLockTest");

    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      writeLock.lock();
      barrier2.await();
      barrier2.await();
      writeLock.unlock();
      barrier2.await();
    } else {
      barrier2.await();
      int count = 0;
      for (int i = 0; i < 10; i++) {
        if (!readLock.tryLock()) {
          if (lock.isWriteLocked()) {
            count++;
          }
        }
      }
      barrier2.await();
      Assert.assertEquals(10, count);
      barrier2.await();
      count = 0;
      for (int i = 0; i < 10; i++) {
        if (!readLock.tryLock()) {
          if (lock.isWriteLocked()) {
            count++;
          }
        }
      }
      Assert.assertEquals(0, count);
    }
    barrier.await();
  }

  private void readWriteLockTest(final int index, final boolean fair) throws Exception {
    basicTryReadLockTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    basicTryReadWriteLockTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    basicTryWriteReadLockTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    basicTryWriteLockTimeoutTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    tryReadLockMultiNodeTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    tryWriteLockMultiNodeTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    tryReadWriteLockSingleNodeTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    if (!Vm.isIBM()) {
      tryReadWriteLockSingleNodeTest(index, new ReentrantReadWriteLock());
    }

    basicSingleNodeReadThenWriteLockingTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    basicSingleNodeReadThenWriteLockingTest(index, new ReentrantReadWriteLock());
    basicMultiNodesReadThenWriteLockingTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    modifyDataUsingReadLockTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    modifyDataTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    lockDownGradeTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    reentrantReadLockWithWriteTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    basicAPITest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
    Tuple<ReentrantReadWriteLock, Condition> t = getASharedReentrantReadWriteLock(fair, index);
    basicConditionVariableTesting(index, t.getFirst().writeLock(), t.getSecond());
    t = getASharedReentrantReadWriteLock(fair, index);
    basicConditionVariableWaitTesting(index, t.getFirst(), t.getSecond());
    t = getASharedReentrantReadWriteLock(fair, index);
    singleNodeConditionVariableTesting(index, t.getFirst().writeLock(), t.getSecond());

    tryLockTest(index, getASharedReentrantReadWriteLock(fair, index).getFirst());
  }

  private Tuple<ReentrantReadWriteLock, Condition> getASharedReentrantReadWriteLock(final boolean fair, final int index)
      throws InterruptedException, BrokenBarrierException {
    ReentrantReadWriteLock rrwl;
    Tuple<ReentrantReadWriteLock, Condition> tuple;
    if (index == 0) {
      rrwl = new ReentrantReadWriteLock(fair);
      tuple = new Tuple<ReentrantReadWriteLock, Condition>(rrwl, rrwl.writeLock().newCondition());
      synchronized (shareableLockList) {
        shareableLockList.clear();
        shareableLockList.add(tuple);
      }
      barrier.await();
    } else {
      barrier.await();
      synchronized (shareableLockList) {
        tuple = (Tuple) shareableLockList.get(0);
      }
    }
    return tuple;
  }

  private void unsharedToSharedTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "unsharedToSharedTest");

    if (index == 1) {
      ReadLock readLock = lock.readLock();
      readLock.lock();
      try {
        dataRoot.setLock(lock);
        throw new AssertionError("Should have expected a TCObjectNotSharableException.");
      } catch (TCObjectNotSharableException e) {
        // expected
      } finally {
        readLock.unlock();
      }
      Assert.assertFalse(ManagerUtil.isManaged(lock));
    }

    barrier.await();

    if (index == 0) {
      WriteLock writeLock = lock.writeLock();
      writeLock.lock();
      try {
        dataRoot.setLock(lock);
        throw new AssertionError("Should have expected a TCObjectNotSharableException.");
      } catch (TCObjectNotSharableException e) {
        // expected
      } finally {
        writeLock.unlock();
      }
      Assert.assertFalse(ManagerUtil.isManaged(lock));
    }

    barrier.await();
  }

  private void tryReadWriteLockSingleNodeTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "tryReadWriteLockSingleNodeTest");

    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          boolean isLocked = false;
          try {
            localBarrier.await();
            isLocked = readLock.tryLock(2, TimeUnit.SECONDS);
            printTimeStamp("After trying to grap a tryLock in tryReadWriteLockSingleNodeTest");
            assertTryLockResult(!isLocked);
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            unLockIfLocked(readLock, isLocked);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(writeLock, isLocked);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          try {
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t1.start();
      t2.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(4000);
      writeLock.unlock();
      printTimeStamp("After unlocking writeLock in tryReadWriteLockSingleNodeTest");
      localBarrier.await();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(readLock, isLocked);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(writeLock, isLocked);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t1.start();
      t2.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(2000);
      writeLock.unlock();
    }

    barrier.await();
  }

  private void tryWriteLockMultiNodeTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "tryWriteLockMultiNodeTest first test");

    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            barrier5.await();
            boolean isLocked = writeLock.tryLock(20, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            Thread.sleep(2000);
            unLockIfLocked(writeLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            barrier5.await();
            boolean isLocked = writeLock.tryLock(21, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            Thread.sleep(2000);
            unLockIfLocked(writeLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      writeLock.lock();
      localBarrier.await();
      barrier4.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
      barrier4.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(9, TimeUnit.SECONDS);
      barrier5.await();
      Thread.sleep(2000);
      assertTryLockResult(isLocked);
      unLockIfLocked(writeLock, isLocked);
      barrier4.await();
      writeLock.lock();
      Thread.sleep(2000);
      writeLock.unlock();
    }

    barrier.await();
    printTimeStamp(index, "tryWriteLockMultiNodeTest second test");

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(4);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(8, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(writeLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(9001, TimeUnit.MILLISECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(writeLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      Thread t3 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(10, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(writeLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      t1.start();
      t2.start();
      t3.start();
      writeLock.lock();
      localBarrier.await();
      barrier4.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(11, TimeUnit.SECONDS);
      assertTryLockResult(isLocked);
      unLockIfLocked(writeLock, isLocked);
    }
    barrier.await();
    printTimeStamp(index, "tryWriteLockMultiNodeTest third test");

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(readLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(8002, TimeUnit.MILLISECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(readLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t1.start();
      t2.start();
      writeLock.lock();
      localBarrier.await();
      barrier4.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10002, TimeUnit.MILLISECONDS);
      assertTryLockResult(isLocked);
      unLockIfLocked(writeLock, isLocked);
    }

    barrier.await();
    printTimeStamp(index, "tryWriteLockMultiNodeTest fourth test");

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = writeLock.tryLock(4001, TimeUnit.MILLISECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(writeLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      barrier4.await();
      Thread.sleep(1000);
      localBarrier.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10003, TimeUnit.MILLISECONDS);
      assertTryLockResult(isLocked);
      unLockIfLocked(writeLock, isLocked);
    }

    barrier.await();
    printTimeStamp(index, "tryWriteLockMultiNodeTest fifth test");

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4003, TimeUnit.MILLISECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(readLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      barrier4.await();
      Thread.sleep(1000);
      localBarrier.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10004, TimeUnit.MILLISECONDS);
      assertTryLockResult(isLocked);
      unLockIfLocked(writeLock, isLocked);
    }

    barrier.await();
    printTimeStamp(index, "tryWriteLockMultiNodeTest sixth test");

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4004, TimeUnit.MILLISECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(readLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(1000);
      barrier4.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else if (index == 1) {
      barrier4.await();
      boolean isLocked = writeLock.tryLock(10005, TimeUnit.MILLISECONDS);
      assertTryLockResult(isLocked);
      unLockIfLocked(writeLock, isLocked);
    }

    barrier.await();
    printTimeStamp(index, "tryWriteLockMultiNodeTest finish");

  }

  private void tryReadLockMultiNodeTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "tryReadLockMultiNodeTest");

    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
            assertTryLockResult(isLocked);
            unLockIfLocked(readLock, isLocked);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(1000);
      barrier2.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else {
      barrier2.await();
      writeLock.lock();
      writeLock.unlock();
    }

    barrier.await();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            writeLock.lock();
            Thread.sleep(2001);
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }

        }
      });

      t1.start();
      writeLock.lock();
      localBarrier.await();
      Thread.sleep(1000);
      barrier2.await();
      Thread.sleep(2000);
      writeLock.unlock();
      localBarrier.await();
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock(10, TimeUnit.SECONDS);
      assertTryLockResult(isLocked);
      unLockIfLocked(readLock, isLocked);
    }

    barrier.await();

    if (index == 0) {
      readLock.lock();
      try {
        barrier2.await();
        barrier2.await();
      } finally {
        readLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock(1000, TimeUnit.MILLISECONDS); // We cannot use tryLock() here due to greedy
      // lock problem for tryLock().
      try {
        barrier2.await();
        assertTryLockResult(isLocked);
      } finally {
        unLockIfLocked(readLock, isLocked);
      }
    }
    barrier.await();

    if (index == 0) {
      writeLock.lock();
      try {
        barrier2.await();
        barrier2.await();
      } finally {
        writeLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock();
      barrier2.await();
      assertTryLockResult(!isLocked);
      unLockIfLocked(readLock, isLocked);
    }

    barrier.await();

    if (index == 0) {
      writeLock.lock();
      try {
        barrier2.await();
        Thread.sleep(2000);
      } finally {
        writeLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock(4, TimeUnit.SECONDS);
      try {
        assertTryLockResult(isLocked);
      } finally {
        unLockIfLocked(readLock, isLocked);
      }
    }
    barrier.await();

    if (index == 0) {
      writeLock.lock();
      try {
        barrier2.await();
        Thread.sleep(4000);
      } finally {
        writeLock.unlock();
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock(1, TimeUnit.SECONDS);
      assertTryLockResult(!isLocked);
      unLockIfLocked(readLock, isLocked);
    }
    barrier.await();
  }

  private void basicConditionVariableWaitTesting(final int index, final ReentrantReadWriteLock lock,
                                                 final Condition condition) throws Exception {
    printTimeStamp(index, "basicConditionVariableWaitTesting");

    WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      Thread thread = new Thread(new TestRunnable1(writeLock, condition));
      thread.start();
    }

    barrier.await();

    if (index == 0) {
      Thread.sleep(2000); // Sleep so that the TestRunnable1 thread can pick up.
      Assert.assertEquals(1, lock.getWaitQueueLength(condition));
      Assert.assertTrue(lock.hasWaiters(condition));
    }

    barrier.await();

    if (index == 0) {
      writeLock.lock();
      try {
        condition.signalAll();
      } finally {
        writeLock.unlock();
      }
    }

    barrier.await();

    if (index == 0) {
      Assert.assertEquals(0, lock.getWaitQueueLength(condition));
    }

    barrier.await();
  }

  private void singleNodeConditionVariableTesting(final int index, final WriteLock lock, final Condition condition)
      throws Exception {
    printTimeStamp(index, "singleNodeConditionVariableTesting");

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(3);
      final List localQueue = new LinkedList();

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            doPutter(1, lock, condition, localQueue, 1);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            doGetter(2, lock, condition, localQueue);
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });

      t1.start();
      t2.start();
      localBarrier.await();
    }

    barrier.await();
  }

  private void basicConditionVariableTesting(final int index, final WriteLock lock, final Condition condition)
      throws Exception {
    printTimeStamp(index, "basicConditionVariableTesting");

    final long id = new Long(getApplicationId()).longValue();

    if (index == 0) {
      doPutter(id, lock, condition, queue, numOfGetters);
    } else {
      doGetter(id, lock, condition, queue);
    }

    barrier.await();
  }

  private void doPutter(final long id, final WriteLock lock, final Condition condition, final List Q, final int getters)
      throws Exception {
    Thread.currentThread().setName("PUTTER-" + id);

    for (int i = 0; i < putsCount; i++) {
      lock.lock();
      try {
        System.err.println("PUTTER-" + id + " Putting " + i);
        Q.add(new WorkItem(String.valueOf(i)));
        if (i % 2 == 0) {
          condition.signalAll();
        } else {
          condition.signal();
        }
      } finally {
        lock.unlock();
      }
    }

    for (int i = 0; i < getters; i++) {
      lock.lock();
      try {
        Q.add(WorkItem.STOP);
        condition.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }

  private void doGetter(final long id, final WriteLock lock, final Condition condition, final List Q) throws Exception {
    Thread.currentThread().setName("GETTER-" + id);

    int i = 0;
    while (true) {
      lock.lock();
      lock.lock();
      try {
        while (Q.size() == 0) {
          int choice = i % 4;
          switch (choice) {
            case 0:
              condition.await();
              break;
            case 1:
              condition.awaitUninterruptibly();
              break;
            case 2:
              long millis = random.nextInt(10000);
              condition.await(millis, TimeUnit.MILLISECONDS);
              break;
            case 3:
              long nanos = random.nextInt(10000);
              condition.awaitNanos(nanos);
              break;
          }
          i++;
        }
        WorkItem wi = (WorkItem) Q.remove(0);
        System.err.println("GETTER- " + id + ManagerUtil.getClientID() + " removes " + wi);
        if (wi.isStop()) { return; }

      } finally {
        lock.unlock();
        lock.unlock();
      }
    }
  }

  private void basicAPITest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "basicAPITest");

    WriteLock writeLock = lock.writeLock();
    ReadLock readLock = lock.readLock();
    if (index == 0) {
      writeLock.lock();
      writeLock.lock();
      writeLock.lock();
      barrier4.await();
      Thread.sleep(2000);
      if (!isCrashTest) {
        Assert.assertTrue(lock.hasQueuedThreads());
      }
      Assert.assertEquals(3, lock.getWriteHoldCount());
      writeLock.unlock();
      Assert.assertEquals(2, lock.getWriteHoldCount());
      writeLock.unlock();
      Assert.assertEquals(1, lock.getWriteHoldCount());
      writeLock.unlock();
      barrier4.await();
    } else if (index == 1) {
      barrier4.await();
      readLock.lock();
      readLock.lock();
      barrier4.await();
      Thread.sleep(2000);
      Assert.assertEquals(2, lock.getReadLockCount());
      readLock.unlock();
      Assert.assertEquals(1, lock.getReadLockCount());
      readLock.unlock();
    }
    barrier.await();

    Assert.assertFalse(lock.hasQueuedThreads());

    barrier.await();

    if (index == 0) {
      writeLock.lock();
      barrier2.await();
      Thread.sleep(4000);
      Assert.assertEquals(2, lock.getQueueLength());
      Assert.assertTrue(lock.isWriteLocked());
      Assert.assertTrue(lock.isWriteLockedByCurrentThread());
      writeLock.unlock();
    } else if (index == 1) {
      barrier2.await();
      readLock.lock();
      readLock.unlock();
    } else {
      barrier2.await();
      writeLock.lock();
      writeLock.unlock();
    }

    barrier.await();
  }

  private void reentrantReadLockWithWriteTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "reentrantReadLockWithWriteTest");

    final WriteLock writeLock = lock.writeLock();
    final ReadLock readLock = lock.readLock();
    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(2);
      Thread t1 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            writeLock.lock();
            writeLock.unlock();
            localBarrier.await();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      });
      readLock.lock();
      t1.start();
      localBarrier.await();
      readLock.lock();
      readLock.unlock();
      readLock.unlock();
      localBarrier.await();
    }

    barrier.await();
  }

  private void lockDownGradeTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "lockDownGradeTest");

    final WriteLock writeLock = lock.writeLock();
    final ReadLock readLock = lock.readLock();
    if (index == 0) {
      writeLock.lock();
      readLock.lock();
      writeLock.unlock();
      readLock.unlock();
    }
    barrier.await();
  }

  private void basicTryWriteLockTimeoutTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "basicTryWriteLockTimeoutTest");

    final WriteLock writeLock = lock.writeLock();
    if (index == 0) {
      boolean isLocked = writeLock.tryLock(1000, TimeUnit.SECONDS);
      try {
        barrier2.await();
        assertTryLockResult(isLocked);
        printTimeStamp("Client " + ManagerUtil.getClientID() + ", index: " + index + " enter first barrier3");
        barrier3.await();
      } finally {
        unLockIfLocked(writeLock, isLocked);
      }
      printTimeStamp("Client " + ManagerUtil.getClientID() + ", index: " + index + " enter last barrier3");
      barrier3.await();
    } else {
      barrier2.await();
      boolean isLocked = writeLock.tryLock(5, TimeUnit.SECONDS);
      assertTryLockResult(!isLocked);
      unLockIfLocked(writeLock, isLocked);
      printTimeStamp("Client " + ManagerUtil.getClientID() + ", index: " + index + " enter first barrier3");
      barrier3.await();
      printTimeStamp("Client " + ManagerUtil.getClientID() + ", index: " + index + " enter last barrier3");
      barrier3.await();
    }

    barrier.await();
  }

  private void modifyDataUsingReadLockTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "modifyDataUsingReadLockTest");

    ReadLock readLock = lock.readLock();

    if (index == 0) {
      readLock.lock();
      try {
        dataRoot.putData("key1", "value1");
        throw new AssertionError("Should have thrown a ReadOnlyException.");
      } catch (ReadOnlyException re) {
        // expected;
      } finally {
        readLock.unlock();
      }
    }

    barrier.await();
  }

  private void modifyDataTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "modifyDataTest");

    ReadLock readLock = lock.readLock();
    WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      writeLock.lock();
      try {
        dataRoot.putData("key2", "value2");
      } finally {
        writeLock.unlock();
      }
      barrier2.await();
    } else {
      barrier2.await();
      readLock.lock();
      try {
        Object data = dataRoot.getData("key2");
        Assert.assertEquals("value2", data);
      } finally {
        readLock.unlock();
      }
    }

    barrier.await();
  }

  private void basicTryWriteReadLockTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "basicTryWriteReadLockTest");

    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      boolean isLocked = writeLock.tryLock();
      barrier2.await();
      try {
        assertTryLockResult(isLocked);
      } finally {
        barrier3.await();
        unLockIfLocked(writeLock, isLocked);
      }
    } else {
      barrier2.await();
      boolean isLocked = readLock.tryLock();
      assertTryLockResult(!isLocked);
      unLockIfLocked(readLock, isLocked);
      barrier3.await();
    }

    barrier.await();
  }

  private void basicTryReadWriteLockTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "basicTryReadWriteLockTest");

    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      barrier2.await();
      boolean isLocked = writeLock.tryLock();
      assertTryLockResult(!isLocked);
      unLockIfLocked(writeLock, isLocked);
      barrier3.await();
    } else {
      boolean isLocked = readLock.tryLock();
      try {
        barrier2.await();
        assertTryLockResult(isLocked);
      } finally {
        barrier3.await();
        unLockIfLocked(readLock, isLocked);
      }
    }

    barrier.await();
  }

  private void basicTryReadLockTest(final int index, final ReentrantReadWriteLock lock) throws Exception {
    printTimeStamp(index, "basicTryReadLockTest");

    final ReadLock readLock = lock.readLock();
    boolean isLocked = readLock.tryLock();
    try {
      barrier2.await();
      assertTryLockResult(isLocked);
    } finally {
      unLockIfLocked(readLock, isLocked);
    }
    barrier.await();
  }

  private void basicSingleNodeReadThenWriteLockingTest(final int index, final ReentrantReadWriteLock lock)
      throws Exception {
    printTimeStamp(index, "basicSingleNodeReadThenWriteLockingTest");

    final ReadLock readLock = lock.readLock();
    final WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      final CyclicBarrier localBarrier = new CyclicBarrier(4);
      final CyclicBarrier localBarrier2 = new CyclicBarrier(5);

      Thread t1 = new Thread(new Runnable() {
        public void run() {
          writeLock.lock();
          try {
            localBarrier.await();
            Thread.sleep(5000);
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          } finally {
            writeLock.unlock();
          }
          try {
            localBarrier2.await();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            readLock.lock();
            try {
              Thread.sleep(2000);
            } finally {
              readLock.unlock();
            }
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
          try {
            localBarrier2.await();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      });

      Thread t3 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            readLock.lock();
            try {
              Thread.sleep(2000);
            } finally {
              readLock.unlock();
            }
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
          try {
            localBarrier2.await();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      });

      Thread t4 = new Thread(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            writeLock.lock();
            try {
              Thread.sleep(2000);
            } finally {
              writeLock.unlock();
            }
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
          try {
            localBarrier2.await();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        }
      });
      t1.start();
      t2.start();
      t3.start();
      t4.start();

      localBarrier2.await();
    }

    barrier.await();
  }

  private void basicMultiNodesReadThenWriteLockingTest(final int index, final ReentrantReadWriteLock lock)
      throws Exception {
    printTimeStamp(index, "basicMultiNodesReadThenWriteLockingTest");

    ReadLock readLock = lock.readLock();
    WriteLock writeLock = lock.writeLock();

    if (index == 0) {
      writeLock.lock();
      try {
        barrier2.await();
        Thread.sleep(5000);
      } finally {
        writeLock.unlock();
      }
    } else if (index == 1 || index == 2) {
      barrier2.await();
      readLock.lock();
      try {
        Thread.sleep(2000);
      } finally {
        readLock.unlock();
      }
    } else if (index == 3) {
      barrier2.await();
      writeLock.lock();
      try {
        Thread.sleep(3000);
      } finally {
        writeLock.unlock();
      }
    }

    barrier.await();
  }

  private void unLockIfLocked(final Lock lock, final boolean isLocked) {
    if (isLocked) {
      lock.unlock();
    }
  }

  private void assertTryLockResult(final boolean isLocked) {
    if (!isCrashTest) {
      Assert.assertTrue(isLocked);
    }
  }

  private void printTimeStamp(final int index, final String methodName) throws Exception {
    if (index == 0) {
      printTimeStamp(methodName);
    }

    barrier.await();
  }

  private void printTimeStamp(final String methodName) throws Exception {
    System.err.println("Running " + methodName + " -- time: " + (new Date()));
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ReentrantReadWriteLockTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("barrier3", "barrier3");
    spec.addRoot("barrier4", "barrier4");
    spec.addRoot("barrier5", "barrier5");
    spec.addRoot("dataRoot", "dataRoot");
    spec.addRoot("queue", "queue");
    spec.addRoot("shareableLockList", "shareableLockList");
  }

  private static class Tuple<E1, E2> {

    private final E1 e1;
    private final E2 e2;

    public Tuple(final E1 e1, final E2 e2) {
      this.e1 = e1;
      this.e2 = e2;
    }

    public E1 getFirst() {
      return e1;
    }

    public E2 getSecond() {
      return e2;
    }
  }

  private static class DataRoot {
    private final Map              store = new HashMap();
    private ReentrantReadWriteLock lock;

    public void putData(final Object key, final Object value) {
      store.put(key, value);
    }

    public Object getData(final Object key) {
      return store.get(key);
    }

    public synchronized void setLock(final ReentrantReadWriteLock lock) {
      this.lock = lock;
    }

    @SuppressWarnings("unused")
    public synchronized ReentrantReadWriteLock getLock() {
      return lock;
    }

  }

  private static class WorkItem {
    static final WorkItem STOP = new WorkItem("STOP");

    private final String  name;

    WorkItem(final String name) {
      this.name = name;
    }

    boolean isStop() {
      return STOP.name.equals(name);
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static class TestRunnable1 implements Runnable {
    private final WriteLock lock;
    private final Condition conditionObject;

    public TestRunnable1(final WriteLock lock, final Condition conditionObject) {
      this.lock = lock;
      this.conditionObject = conditionObject;
    }

    public void run() {
      lock.lock();
      try {
        conditionObject.await();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      } finally {
        lock.unlock();
      }
    }
  }

}
