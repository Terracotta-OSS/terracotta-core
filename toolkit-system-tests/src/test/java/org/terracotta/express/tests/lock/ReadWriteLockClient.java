package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.express.tests.util.ClusteredStringBuilder;
import org.terracotta.express.tests.util.ClusteredStringBuilderFactory;
import org.terracotta.express.tests.util.ClusteredStringBuilderFactoryImpl;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;

public class ReadWriteLockClient extends ClientBase {
  private ClusteredStringBuilderFactory csbFactory;
  private int                           index = -1;

  public ReadWriteLockClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    this.index = getBarrierForAllClients().await();
    csbFactory = new ClusteredStringBuilderFactoryImpl(toolkit);
    basicWriteLockUnlock(toolkit);
    basicReadLockUnlock(toolkit);
    basicTryReadLockUnlock(toolkit);
    basicTryWriteLockUnlock(toolkit);
    basicCondition(toolkit);
  }

  private void basicWriteLockUnlock(Toolkit toolkit) throws InterruptedException, BrokenBarrierException, IOException {
    if (index == 0) {
      for (int i = 0; i < 10; i++) {
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicWriteLockUnlock" + i);
        ReadWriteLock lock = toolkit.getReadWriteLock("basicWriteLockUnlockBucket" + i);
        System.err.println(" lock " + lock.getClass().getName());
        lock.writeLock().lock();
        try {
          getBarrierForAllClients().await();
          bucket.append("l1");
        } finally {
          lock.writeLock().unlock();
        }
        getBarrierForAllClients().await();

        String str = bucket.toString();
        Assert.assertEquals("l1l2", str);

        System.out.println("basicWriteLockUnlock " + i + " passed. String = " + str);
        getBarrierForAllClients().await();
      }
    } else if (index == 1) {
      for (int i = 0; i < 10; i++) {
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicWriteLockUnlock" + i);
        ReadWriteLock lock = toolkit.getReadWriteLock("basicWriteLockUnlockBucket" + i);
        getBarrierForAllClients().await();
        lock.writeLock().lock();
        try {
          bucket.append("l2");
        } finally {
          lock.writeLock().unlock();
        }
        getBarrierForAllClients().await();

        String str = bucket.toString();
        Assert.assertEquals("l1l2", str);

        System.out.println("basicWriteLockUnlock " + i + " passed. String = " + str);
        getBarrierForAllClients().await();
      }
    }
  }

  private void basicReadLockUnlock(Toolkit toolkit) throws IOException, InterruptedException, BrokenBarrierException {
    if (index == 0) {
      for (int i = 0; i < 10; i++) {
        ReadWriteLock lock = toolkit.getReadWriteLock("basicReadLockUnlock" + i);
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicReadLockUnlockBucket" + i);
        lock.readLock().lock();
        try {
          bucket.append("r");
          getBarrierForAllClients().await();
        } finally {
          lock.readLock().unlock();
        }
        getBarrierForAllClients().await();
        String str = bucket.toString();
        Assert.assertEquals("rr", str);

        System.out.println("basicReadLockUnlock " + i + " passed. String = " + str);
        getBarrierForAllClients().await();
      }
    } else if (index == 1) {

      for (int i = 0; i < 10; i++) {
        ReadWriteLock lock = toolkit.getReadWriteLock("basicReadLockUnlock" + i);
        // Barrier barrier = toolkit.getBarrier("basicReadLockUnlockBarrier" + i, 2);
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicReadLockUnlockBucket" + i);
        lock.readLock().lock();

        try {
          bucket.append("r");
          getBarrierForAllClients().await();
        } finally {
          lock.readLock().unlock();
        }
        getBarrierForAllClients().await();
        String str = bucket.toString();
        Assert.assertEquals("rr", str);

        System.out.println("basicReadLockUnlock " + i + " passed. String = " + str);
        getBarrierForAllClients().await();
      }

    }
  }

  private void basicTryReadLockUnlock(Toolkit toolkit) throws IOException, InterruptedException, BrokenBarrierException {
    if (index == 0) {
      for (int i = 0; i < 10; i++) {
        ReadWriteLock lock = toolkit.getReadWriteLock("basicTryReadLockUnlock" + i);
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicTryReadLockUnlockBucket" + i);

        lock.readLock().lock();
        getBarrierForAllClients().await();

        try {
          bucket.append("r");
          getBarrierForAllClients().await();
        } finally {
          lock.readLock().unlock();
        }
        getBarrierForAllClients().await();
        String str = bucket.toString();
        Assert.assertEquals("rr", str);

        System.out.println("basicTryReadLockUnlock " + i + " passed. String = " + str);
        getBarrierForAllClients().await();
      }
    } else if (index == 1) {

      for (int i = 0; i < 10; i++) {
        ReadWriteLock lock = toolkit.getReadWriteLock("basicTryReadLockUnlock" + i);
        // Barrier barrier = toolkit.getBarrier("basicTryReadLockUnlockBarrier" + i, 2);
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicTryReadLockUnlockBucket" + i);

        getBarrierForAllClients().await();
        boolean acquired = lock.readLock().tryLock();
        Assert.assertTrue(acquired);

        try {
          bucket.append("r");
          getBarrierForAllClients().await();
        } finally {
          lock.readLock().unlock();
        }
        getBarrierForAllClients().await();
        String str = bucket.toString();
        Assert.assertEquals("rr", str);

        System.out.println("basicTryReadLockUnlock " + i + " passed. String = " + str);
        getBarrierForAllClients().await();
      }

    }
  }

  private void basicTryWriteLockUnlock(Toolkit toolkit) throws IOException, InterruptedException,
      BrokenBarrierException {
    if (index == 0) {
      for (int i = 0; i < 10; i++) {
        ReadWriteLock lock = toolkit.getReadWriteLock("basicTryWriteLockUnlock" + i);
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicTryWriteLockUnlockBucket" + i);

        lock.writeLock().lock();
        getBarrierForAllClients().await(); // 1

        try {
          bucket.append("r");
          getBarrierForAllClients().await(); // 2
        } finally {
          lock.writeLock().unlock();
        }

        getBarrierForAllClients().await(); // 3
        getBarrierForAllClients().await(); // 4

        String str = bucket.toString();
        Assert.assertEquals("rr", str);

        System.out.println("basicTryWriteLockUnlock " + i + " passed. String = " + str);
        getBarrierForAllClients().await(); // 5
      }
    } else if (index == 1) {

      for (int i = 0; i < 10; i++) {
        ReadWriteLock lock = toolkit.getReadWriteLock("basicTryWriteLockUnlock" + i);
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicTryWriteLockUnlockBucket" + i);
        getBarrierForAllClients().await(); // 1
        boolean acquired = lock.writeLock().tryLock();
        Assert.assertFalse(acquired);

        getBarrierForAllClients().await(); // 2
        getBarrierForAllClients().await(); // 3
        acquired = lock.writeLock().tryLock();
        Assert.assertTrue(acquired);

        try {
          bucket.append("r");
        } finally {
          lock.writeLock().unlock();
        }
        getBarrierForAllClients().await(); // 4

        String str = bucket.toString();
        Assert.assertEquals("rr", str);

        System.out.println("basicTryWriteLockUnlock " + i + " passed. String = " + str);
        getBarrierForAllClients().await(); // 5
      }

    }
  }

  private void basicCondition(Toolkit toolkit) throws IOException, InterruptedException, BrokenBarrierException {
    if (index == 0) {
      for (int i = 0; i < 10; i++) {
        ToolkitReadWriteLock lock = toolkit.getReadWriteLock("basicConditionLock" + i);
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicConditionTextBucket" + i);
        ToolkitLock writeLock = lock.writeLock();
        writeLock.lock();
        bucket.append("a");
        getBarrierForAllClients().await();

        try {
          Condition condition = writeLock.getCondition();
          condition.await();
          bucket.append("c");
        } finally {
          lock.writeLock().unlock();
        }

        getBarrierForAllClients().await();

        String str = bucket.toString();
        Assert.assertEquals("abc", str);

        System.out.println("basicCondition " + i + " passed. String = " + str);
        getBarrierForAllClients().await();
      }
    } else if (index == 1) {

      for (int i = 0; i < 10; i++) {
        ToolkitReadWriteLock lock = toolkit.getReadWriteLock("basicConditionLock" + i);
        // Barrier barrier = toolkit.getBarrier("basicConditionBarrier" + i, 2);
        ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("basicConditionTextBucket" + i);
        getBarrierForAllClients().await();
        ToolkitLock writeLock = lock.writeLock();
        writeLock.lock();

        try {
          Condition condition = writeLock.getCondition();
          condition.signalAll();
          bucket.append("b");
        } finally {
          lock.writeLock().unlock();
        }

        getBarrierForAllClients().await();

        String str = bucket.toString();
        Assert.assertEquals("abc", str);

        System.out.println("basicCondition " + i + " passed. String = " + str);
        getBarrierForAllClients().await();
      }

    }
  }
}