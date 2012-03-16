/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.net.ClientID;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class ClientLockTest extends TestCase {

  private static final LockID                LOCK_ID            = new StringLockID("testlock");

  private static final Collection<LockLevel> WRITE_LEVELS;
  private static final Collection<LockLevel> READ_LEVELS;
  static {
    Collection<LockLevel> write = new HashSet<LockLevel>();
    Collection<LockLevel> read = new HashSet<LockLevel>();
    for (LockLevel level : LockLevel.values()) {
      if (level.isWrite()) {
        write.add(level);
      }
      if (level.isRead()) {
        read.add(level);
      }
    }

    WRITE_LEVELS = write;
    READ_LEVELS = read;
  }

  private static final WaitListener          NULL_WAIT_LISTENER = new WaitListener() {
                                                                  public void handleWaitEvent() {
                                                                    //
                                                                  }
                                                                };

  private ClientLock getFreshClientLock() {
    return new ClientLockImpl(LOCK_ID);
  }

  public void testBasicLockFunctionality() {
    ClientLock lock = getFreshClientLock();

    try {
      for (LockLevel level : LockLevel.values()) {
        checkLockQueryMethods(lock, 0, 0);
        lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                    new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0);
      }
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testBasicTryLockFunctionality() {
    ClientLock lock = getFreshClientLock();

    try {
      for (LockLevel level : LockLevel.values()) {
        checkLockQueryMethods(lock, 0, 0);
        Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock, RemoteOperation.TRY_LOCK), new ThreadID(1),
                                       level));
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                    new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0);
      }
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testBasicTryLockWithTimeoutFunctionality() {
    ClientLock lock = getFreshClientLock();

    try {
      for (LockLevel level : LockLevel.values()) {
        checkLockQueryMethods(lock, 0, 0);
        Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock, RemoteOperation.TRY_LOCK), new ThreadID(1),
                                       level, 0));
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                    new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0);
      }
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    } catch (InterruptedException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testBasicLockInterruptiblyFunctionality() {
    ClientLock lock = getFreshClientLock();

    try {
      for (LockLevel level : LockLevel.values()) {
        checkLockQueryMethods(lock, 0, 0);
        lock.lockInterruptibly(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                    new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0);
      }
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    } catch (InterruptedException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testLockExclusionProperties() {
    ClientLock lock = getFreshClientLock();

    try {
      for (LockLevel level : WRITE_LEVELS) {
        checkLockQueryMethods(lock, 0, 0);
        lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        for (LockLevel nested : LockLevel.values()) {
          switch (nested) {
            case CONCURRENT:
              Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested));
              lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
              break;
            default:
              Assert.assertFalse(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested));
              break;
          }
        }
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                    new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0);
      }

      for (LockLevel level : READ_LEVELS) {
        checkLockQueryMethods(lock, 0, 0);
        lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        for (LockLevel nested : LockLevel.values()) {
          if (nested.isWrite()) {
            Assert.assertFalse(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested));
          } else {
            Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock, RemoteOperation.TRY_LOCK),
                                           new ThreadID(2), nested));
            checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level), hold(new ThreadID(2), nested));
            lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                        new ThreadID(2), nested);
            checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
          }
        }
        lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                    new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0);
      }
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testLockNestingProperties() {
    ClientLock lock = getFreshClientLock();

    try {
      for (LockLevel level : WRITE_LEVELS) {
        checkLockQueryMethods(lock, 0, 0);
        lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        for (LockLevel nested : LockLevel.values()) {
          Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(1), nested));
          checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level), hold(new ThreadID(1), nested));
          lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.FLUSH), new ThreadID(1), nested);
          checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        }
        lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                    new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0);
      }

      for (LockLevel level : READ_LEVELS) {
        checkLockQueryMethods(lock, 0, 0);
        lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        for (LockLevel nested : LockLevel.values()) {
          if (nested.isWrite()) {
            try {
              lock.lock(new AssertingRemoteLockManager(lock), new ThreadID(1), nested);
              Assert.fail("Expected TCLockUpgradeNotSupportedError");
            } catch (TCLockUpgradeNotSupportedError e) {
              // expected
            }
          } else {
            Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(1), nested));
            checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level), hold(new ThreadID(1), nested));
            lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(1), nested);
            checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
          }
        }
        lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                    new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0);
      }
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testTimeoutOfTryLock() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      for (LockLevel nested : LockLevel.values()) {
        try {
          switch (nested) {
            case CONCURRENT:
              Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested, 1000));
              lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
              break;
            default:
              long start = System.currentTimeMillis();
              Assert.assertFalse(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested, 1000));
              long duration = System.currentTimeMillis() - start;
              Assert.assertTrue(duration > 500);
              Assert.assertTrue(duration < 2000);
              break;
          }
          checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
        } catch (InterruptedException e) {
          Assert.failure("Unexpected Exception ", e);
        }
      }
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.FLUSH, RemoteOperation.UNLOCK), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testPreInterruptingTimedTryLock() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      for (LockLevel nested : LockLevel.values()) {
        try {
          switch (nested) {
            case CONCURRENT:
              Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested, 1000));
              lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
              break;
            default:
              Thread.currentThread().interrupt();
              try {
                lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested, 1000);
                Assert.fail("Expected InterruptedException");
              } catch (InterruptedException e) {
                // expected
              }
              break;
          }
        } catch (InterruptedException e) {
          Assert.failure("Unexpected Exception ", e);
        }
      }
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.FLUSH, RemoteOperation.UNLOCK), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testInterruptingTimedTryLock() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      for (LockLevel nested : LockLevel.values()) {
        try {
          switch (nested) {
            case CONCURRENT:
              Assert.assertTrue(lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested, 10000));
              lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
              break;
            default:
              try {
                final Thread t = Thread.currentThread();
                new Thread() {
                  @Override
                  public void run() {
                    try {
                      Thread.sleep(2000);
                    } catch (InterruptedException e) {
                      // ignore
                    } finally {
                      t.interrupt();
                    }
                  }
                }.start();
                lock.tryLock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested, 10000);
                Assert.fail("Expected InterruptedException");
              } catch (InterruptedException e) {
                // expected
              }
              break;
          }
        } catch (InterruptedException e) {
          Assert.failure("Unexpected Exception ", e);
        }
      }
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.FLUSH, RemoteOperation.UNLOCK), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testPreInterruptingInterruptibleLock() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      for (LockLevel nested : LockLevel.values()) {
        try {
          switch (nested) {
            case CONCURRENT:
              lock.lockInterruptibly(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
              lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
              break;
            default:
              Thread.currentThread().interrupt();
              try {
                lock.lockInterruptibly(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
                lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
                Assert.fail("Expected InterruptedException");
              } catch (InterruptedException e) {
                // expected
              }
              break;
          }
        } catch (InterruptedException e) {
          Assert.failure("Unexpected Exception ", e);
        }
      }
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.FLUSH, RemoteOperation.UNLOCK), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testInterruptingInterruptibleLock() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      for (LockLevel nested : LockLevel.values()) {
        try {
          switch (nested) {
            case CONCURRENT:
              lock.lockInterruptibly(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
              lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
              break;
            default:
              try {
                final Thread t = Thread.currentThread();
                new Thread() {
                  @Override
                  public void run() {
                    try {
                      Thread.sleep(2000);
                    } catch (InterruptedException e) {
                      // ignore
                    } finally {
                      t.interrupt();
                    }
                  }
                }.start();
                lock.lockInterruptibly(new AssertingRemoteLockManager(lock), new ThreadID(2), nested);
                Assert.fail("Expected InterruptedException");
              } catch (InterruptedException e) {
                // expected
              }
              break;
          }
        } catch (InterruptedException e) {
          Assert.failure("Unexpected Exception ", e);
        }
      }
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.FLUSH, RemoteOperation.UNLOCK), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testIllegalLockUnlockSequences() {
    ClientLock lock = getFreshClientLock();

    try {
      for (LockLevel lockLevel : LockLevel.values()) {
        for (LockLevel unlockLevel : LockLevel.values()) {
          if (lockLevel == unlockLevel) continue;

          checkLockQueryMethods(lock, 0, 0);
          lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), lockLevel);
          checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), lockLevel));
          try {
            switch (unlockLevel) {
              case CONCURRENT:
                lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(1), unlockLevel);
                break;
              default:
                try {
                  lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(1), unlockLevel);
                  Assert.fail("Expected IllegalMonitorStateException");
                } catch (IllegalMonitorStateException e) {
                  // expected
                }
            }
          } finally {
            checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), lockLevel));
            lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                        new ThreadID(1), lockLevel);
            checkLockQueryMethods(lock, 0, 0);
          }
        }
      }
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testGreedyReadFunctionality() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.READ));
      lock.lock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(2), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.READ), hold(new ThreadID(2), LockLevel.READ));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(2), LockLevel.READ));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(2), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0);

      lock.lock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(3), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(3), LockLevel.READ));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(3), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0);

      lock
          .recall(
                  new AssertingGreedyRemoteLockManager(lock, RemoteOperation.TXN_FLUSHED, RemoteOperation.RECALL_COMMIT),
                  ServerLockLevel.WRITE, 0, false);

      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.READ));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0);

      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.FLUSH, RemoteOperation.RECALL_COMMIT),
                new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testGreedyWriteFunctionality() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.lock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE), hold(new ThreadID(1), LockLevel.READ));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);

      lock.lock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(2), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(2), LockLevel.WRITE));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(2), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);

      lock
          .recall(
                  new AssertingGreedyRemoteLockManager(lock, RemoteOperation.TXN_FLUSHED, RemoteOperation.RECALL_COMMIT),
                  ServerLockLevel.WRITE, 0, false);

      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testWaitNotifyTimesOut() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.wait(new AssertingRemoteLockManager(lock, RemoteOperation.WAIT, RemoteOperation.FLUSH), NULL_WAIT_LISTENER,
                new ThreadID(1), null, 500);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    } catch (InterruptedException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testWaitNotifyWhenGreedyTimesOut() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.wait(new AssertingGreedyRemoteLockManager(lock), NULL_WAIT_LISTENER, new ThreadID(1), null, 500);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    } catch (InterruptedException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testWaitNotifyIsNotifiable() {
    final ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            // ignore
          }
          lock.notified(new ThreadID(1));
          checkLockQueryMethods(lock, 1, 0);
          try {
            lock.award(new AssertingRemoteLockManager(lock), new ThreadID(1), ServerLockLevel.WRITE);
          } catch (GarbageLockException e) {
            Assert.failure("Unexpected Exception ", e);
          }
        }
      }.start();

      lock.wait(new AssertingRemoteLockManager(lock, RemoteOperation.WAIT, RemoteOperation.FLUSH), NULL_WAIT_LISTENER,
                new ThreadID(1), null);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    } catch (InterruptedException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testNestedWaitNotifyIsNotifiable() {
    final ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE), hold(new ThreadID(1), LockLevel.WRITE));
      new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            // ignore
          }
          lock.notified(new ThreadID(1));
          checkLockQueryMethods(lock, 2, 0);
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            // ignore
          }
          try {
            lock.award(new AssertingRemoteLockManager(lock), new ThreadID(1), ServerLockLevel.WRITE);
          } catch (GarbageLockException e) {
            Assert.failure("Unexpected Exception ", e);
          }
        }
      }.start();

      lock.wait(new AssertingRemoteLockManager(lock, RemoteOperation.WAIT, RemoteOperation.FLUSH), NULL_WAIT_LISTENER,
                new ThreadID(1), null);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE), hold(new ThreadID(1), LockLevel.WRITE));
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH), new ThreadID(1),
                  LockLevel.WRITE);
      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    } catch (InterruptedException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testGreedyWaitNotifyIsNotifiable() {
    final ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      final Thread t = Thread.currentThread();
      new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            // ignore
          }

          try {
            lock.lock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(2), LockLevel.WRITE);
            checkLockQueryMethods(lock, 0, 1, hold(new ThreadID(2), LockLevel.WRITE));
            lock.notify(new AssertingRemoteLockManager(lock), new ThreadID(2), null);
            checkLockQueryMethods(lock, 1, 0, hold(new ThreadID(2), LockLevel.WRITE));
            lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(2), LockLevel.WRITE);
          } catch (Throwable e) {
            e.printStackTrace();
            t.destroy();
          }
        }
      }.start();

      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.wait(new AssertingRemoteLockManager(lock), NULL_WAIT_LISTENER, new ThreadID(1), null);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
      lock.unlock(new AssertingRemoteLockManager(lock), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    } catch (InterruptedException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testWaitIllegalMonitorState() {
    ClientLock lock = getFreshClientLock();

    try {
      lock.wait(new AssertingRemoteLockManager(lock), NULL_WAIT_LISTENER, new ThreadID(1), null, 500);
      Assert.fail("Expected IllegalMonitorStateException");
    } catch (IllegalMonitorStateException e) {
      // expected
    } catch (InterruptedException e) {
      Assert.failure("Unexpected Exception ", e);
    }

    for (LockLevel level : LockLevel.values()) {
      if (level.isWrite()) continue;

      try {
        checkLockQueryMethods(lock, 0, 0);
        lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        try {
          lock.wait(new AssertingRemoteLockManager(lock), NULL_WAIT_LISTENER, new ThreadID(1), null, 500);
        } finally {
          checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
          lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                      new ThreadID(1), level);
          checkLockQueryMethods(lock, 0, 0);
        }
        Assert.fail("Expected IllegalMonitorStateException");
      } catch (IllegalMonitorStateException e) {
        // expected
      } catch (InterruptedException e) {
        Assert.failure("Unexpected Exception ", e);
      } catch (GarbageLockException e) {
        Assert.failure("Unexpected Exception ", e);
      }
    }
  }

  public void testNotifyIllegalMonitorState() {
    ClientLock lock = getFreshClientLock();

    try {
      lock.notify(new AssertingRemoteLockManager(lock), new ThreadID(1), null);
      Assert.fail("Expected IllegalMonitorStateException");
    } catch (IllegalMonitorStateException e) {
      // expected
    }

    for (LockLevel level : LockLevel.values()) {
      if (level.isWrite()) continue;

      try {
        checkLockQueryMethods(lock, 0, 0);
        lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        try {
          lock.notify(new AssertingRemoteLockManager(lock), new ThreadID(1), null);
        } finally {
          checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
          lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                      new ThreadID(1), level);
          checkLockQueryMethods(lock, 0, 0);
        }
        Assert.fail("Expected IllegalMonitorStateException");
      } catch (IllegalMonitorStateException e) {
        // expected
      } catch (GarbageLockException e) {
        Assert.failure("Unexpected Exception ", e);
      }
    }
  }

  public void testNotifyAllIllegalMonitorState() {
    ClientLock lock = getFreshClientLock();

    try {
      lock.notifyAll(new AssertingRemoteLockManager(lock), new ThreadID(1), null);
      Assert.fail("Expected IllegalMonitorStateException");
    } catch (IllegalMonitorStateException e) {
      // expected
    }

    for (LockLevel level : LockLevel.values()) {
      if (level.isWrite()) continue;

      try {
        checkLockQueryMethods(lock, 0, 0);
        lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), level);
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
        try {
          lock.notifyAll(new AssertingRemoteLockManager(lock), new ThreadID(1), null);
        } finally {
          checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), level));
          lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH),
                      new ThreadID(1), level);
          checkLockQueryMethods(lock, 0, 0);
        }
        Assert.fail("Expected IllegalMonitorStateException");
      } catch (IllegalMonitorStateException e) {
        // expected
      } catch (GarbageLockException e) {
        Assert.failure("Unexpected Exception ", e);
      }
    }
  }

  public void testLockGarbageCollection() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);

      for (int i = 0; i < 100; i++) {
        checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));
        Assert.assertFalse(lock.tryMarkAsGarbage(new AssertingRemoteLockManager(lock)));
      }

      lock.unlock(new AssertingRemoteLockManager(lock, RemoteOperation.UNLOCK, RemoteOperation.FLUSH), new ThreadID(1),
                  LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);

      Assert.assertFalse(lock.tryMarkAsGarbage(new AssertingRemoteLockManager(lock)));
      Assert.assertTrue(lock.tryMarkAsGarbage(new AssertingRemoteLockManager(lock)));

      try {
        lock.lock(new AssertingRemoteLockManager(lock), new ThreadID(1), LockLevel.WRITE);
        Assert.fail("Expected GarbageLockException");
      } catch (GarbageLockException e) {
        // expected
      }
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }

  }

  public void testLockPinningPreventsGC() {
    ClientLock lock = getFreshClientLock();

    lock.pinLock();

    for (int i = 0; i < 100; i++) {
      checkLockQueryMethods(lock, 0, 0);
      Assert.assertFalse(lock.tryMarkAsGarbage(new AssertingRemoteLockManager(lock)));
    }

    lock.unpinLock();

    Assert.assertTrue(lock.tryMarkAsGarbage(new AssertingRemoteLockManager(lock)));
  }

  public void testGreedyWriteOnReadRecall() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));

      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);

      lock
          .recall(
                  new AssertingGreedyRemoteLockManager(lock, RemoteOperation.TXN_FLUSHED, RemoteOperation.RECALL_COMMIT),
                  ServerLockLevel.READ, 0, false);

      lock.lock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(2), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(2), LockLevel.READ));

      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(2), LockLevel.READ);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  public void testBatchedRecalls() {
    ClientLock lock = getFreshClientLock();

    try {
      checkLockQueryMethods(lock, 0, 0);
      lock.lock(new AssertingGreedyRemoteLockManager(lock, RemoteOperation.LOCK), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0, hold(new ThreadID(1), LockLevel.WRITE));

      lock.unlock(new AssertingGreedyRemoteLockManager(lock), new ThreadID(1), LockLevel.WRITE);
      checkLockQueryMethods(lock, 0, 0);

      lock
          .recall(
                  new AssertingGreedyRemoteLockManager(lock, RemoteOperation.TXN_FLUSHED, RemoteOperation.RECALL_COMMIT),
                  ServerLockLevel.READ, 0, true);

      ThreadUtil.reallySleep(10);
      checkLockQueryMethods(lock, 0, 0);
    } catch (GarbageLockException e) {
      Assert.failure("Unexpected Exception ", e);
    }
  }

  static LockHold hold(ThreadID thread, LockLevel level) {
    return new LockHold(thread, level);
  }

  static class LockHold {
    LockHold(ThreadID thread, LockLevel level) {
      this.thread = thread;
      this.level = level;
    }

    final ThreadID  thread;
    final LockLevel level;
  }

  private void checkLockQueryMethods(ClientLock lock, int pending, int waiting, LockHold... holds) {
    for (LockLevel level : LockLevel.values()) {
      if (level == LockLevel.CONCURRENT) continue;

      int count = 0;
      for (LockHold h : holds) {
        if (h.level == level) {
          count++;
          Assert.assertTrue(lock.isLockedBy(h.thread, level));
        }
      }

      if (count > 0) {
        Assert.assertTrue(lock.isLocked(level));
      }
      Assert.assertEquals("Lock Holds @ " + level, count, lock.holdCount(level));
    }

    Assert.assertEquals(pending, lock.pendingCount());
    Assert.assertEquals(waiting, lock.waitingCount());
  }

  enum RemoteOperation {
    FLUSH, INTERRUPT, LOCK, QUERY, RECALL_COMMIT, TRY_LOCK, UNLOCK, WAIT, TXN_FLUSHED;
  }

  static class AssertingGreedyRemoteLockManager extends AssertingRemoteLockManager {

    public AssertingGreedyRemoteLockManager(ClientLock lock, RemoteOperation... legal) {
      super(lock, legal);
    }

    @Override
    protected void awardLock(final ThreadID thread, final ServerLockLevel level) {
      executor.execute(new Runnable() {
        public void run() {
          try {
            target.award(AssertingGreedyRemoteLockManager.this, ThreadID.VM_ID, level);
          } catch (GarbageLockException e) {
            Assert.failure("Unexpected Exception ", e);
          }
        }
      });
    }
  }

  static class AssertingRemoteLockManager implements RemoteLockManager {

    protected final Executor                  executor = Executors.newSingleThreadExecutor();
    protected final ClientLock                target;
    private final Collection<RemoteOperation> legal;

    public AssertingRemoteLockManager(ClientLock lock, RemoteOperation... legal) {
      this.target = lock;
      this.legal = Arrays.asList(legal);
    }

    public void flush(LockID lock, ServerLockLevel level) {
      Assert.assertTrue(legal.contains(RemoteOperation.FLUSH));
    }

    public ClientID getClientID() {
      return ClientID.NULL_ID;
    }

    public void interrupt(LockID lock, ThreadID thread) {
      Assert.assertTrue(legal.contains(RemoteOperation.INTERRUPT));
    }

    public boolean asyncFlush(LockID lock, LockFlushCallback callback, ServerLockLevel level) {
      Assert.assertTrue(legal.contains(RemoteOperation.TXN_FLUSHED));
      return true;
    }

    public void lock(final LockID lock, final ThreadID thread, final ServerLockLevel level) {
      Assert.assertTrue(legal.contains(RemoteOperation.LOCK));
      awardLock(thread, level);
    }

    public void query(LockID lock, ThreadID thread) {
      Assert.assertTrue(legal.contains(RemoteOperation.QUERY));
    }

    public void recallCommit(LockID lock, Collection<ClientServerExchangeLockContext> lockState, boolean batch) {
      Assert.assertTrue(legal.contains(RemoteOperation.RECALL_COMMIT));
      for (ClientServerExchangeLockContext c : lockState) {
        switch (c.getState().getType()) {
          case PENDING:
            awardLock(c.getThreadID(), c.getState().getLockLevel());
            break;
          default:
            break;
        }
      }
    }

    public void tryLock(final LockID lock, final ThreadID thread, final ServerLockLevel level, final long timeout) {
      Assert.assertTrue(legal.contains(RemoteOperation.TRY_LOCK));
      executor.execute(new Runnable() {
        public void run() {
          try {
            target.award(AssertingRemoteLockManager.this, thread, level);
          } catch (GarbageLockException e) {
            Assert.failure("Unexpected Exception ", e);
          }
        }
      });
    }

    public void unlock(LockID lock, ThreadID thread, ServerLockLevel level) {
      Assert.assertTrue(legal.contains(RemoteOperation.UNLOCK));
    }

    public void wait(LockID lock, ThreadID thread, long waitTime) {
      Assert.assertTrue(legal.contains(RemoteOperation.WAIT));

      if (waitTime == 0) return;

      try {
        TimeUnit.MILLISECONDS.sleep(waitTime);
      } catch (InterruptedException e) {
        // ignore
      }

      notifyLock(thread);
      awardLock(thread, ServerLockLevel.WRITE);
    }

    protected void awardLock(final ThreadID thread, final ServerLockLevel level) {
      executor.execute(new Runnable() {
        public void run() {
          try {
            target.award(AssertingRemoteLockManager.this, thread, level);
          } catch (GarbageLockException e) {
            Assert.failure("Unexpected Exception ", e);
          }
        }
      });
    }

    protected void notifyLock(final ThreadID thread) {
      executor.execute(new Runnable() {
        public void run() {
          target.notified(thread);
        }
      });
    }

    public void waitForServerToReceiveTxnsForThisLock(LockID lock) {
      //
    }

    public void shutdown() {
      //
    }

    public boolean isShutdown() {
      return false;
    }
  }
}
