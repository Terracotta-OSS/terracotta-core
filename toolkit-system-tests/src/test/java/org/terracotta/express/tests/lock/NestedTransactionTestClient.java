/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;

public class NestedTransactionTestClient extends ClientBase {

  public final static int NODE_COUNT = 3;
  private final int       myCount    = 100;
  private List            list1;
  private List            list2;
  private List            list3;
  private ReadWriteLock   lock1, lock2, lock3;

  public NestedTransactionTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    list1 = toolkit.getList("list1", null);
    list2 = toolkit.getList("list2", null);
    list3 = toolkit.getList("list3", null);
    lock1 = toolkit.getReadWriteLock("lock1");
    lock2 = toolkit.getReadWriteLock("lock2");
    lock3 = toolkit.getReadWriteLock("lock3");
    testNestedLocks();
    Lock concurrentLock = ((ToolkitInternal) toolkit).getLock("ConcurrentLock", ToolkitLockTypeInternal.CONCURRENT);
    concurrentLock.lock();
    try {
      testNestedLocks();
    } finally {
      concurrentLock.unlock();
    }

  }

  private void testNestedLocks() throws InterruptedException, BrokenBarrierException {
    int id = getBarrierForAllClients().await();
    if (id == 0) {
      clearAllLists();
    }
    getBarrierForAllClients().await();
    // Add elements to list 1 and list 2 write lock nested inside another write lock
    for (int i = 0; i < myCount; i++) {
      add1("element" + i);
    }

    getBarrierForAllClients().await();
    lock1.readLock().lock();
    try {
      Assert.assertEquals(list1.size(), myCount * getParticipantCount());
    } finally {
      lock1.readLock().unlock();
    }

    lock2.readLock().lock();
    try {
      Assert.assertEquals(list2.size(), myCount * getParticipantCount());

    } finally {
      lock2.readLock().unlock();
    }

    lock3.readLock().lock();
    try {
      Assert.assertEquals(list3.size(), 0);
    } finally {
      lock3.readLock().unlock();
    }

    getBarrierForAllClients().await();
    // move elements from list 1 to list3 , take read lock first and then write lock.
    moveFromList1();
    // move elements from list 2 to list 3 , takes write lock first and then read lock.
    moveFromList2();

    getBarrierForAllClients().await();
    lock1.readLock().lock();
    try {
      Assert.assertEquals(list1.size(), 0);
    } finally {
      lock1.readLock().unlock();
    }

    lock2.readLock().lock();
    try {
      Assert.assertEquals(list2.size(), 0);

    } finally {
      lock2.readLock().unlock();
    }

    lock3.readLock().lock();
    try {
      Assert.assertEquals(list3.size(), myCount * getParticipantCount() * 2);
    } finally {
      lock3.readLock().unlock();
    }
  }

  private void clearAllLists() {
    lock1.writeLock().lock();
    try {
      lock2.writeLock().lock();
      try {
        lock3.writeLock().lock();
        try {
          list1.clear();
          list2.clear();
          list3.clear();
        } finally {
          lock3.writeLock().unlock();
        }
      } finally {
        lock2.writeLock().unlock();
      }
    } finally {
      lock1.writeLock().unlock();
    }

  }

  public void add1(String element) {
    lock1.writeLock().lock();
    try {
      int s = list1.size();
      list1.add(element);
      add2(element);
      Assert.assertEquals(s + 1, list1.size());
      // System.out.println("Added1:"+list1.size());
    } finally {
      lock1.writeLock().unlock();
    }
  }

  public void add3(String element) {
    lock3.writeLock().lock();
    try {
      int s = list3.size();
      list3.add(element);
      Assert.assertEquals(s + 1, list3.size());
      // System.out.println("Added1:"+list1.size());
    } finally {
      lock3.writeLock().unlock();
    }
  }

  public void add2(String element) {
    lock2.writeLock().lock();
    try {
      int s = list2.size();
      list2.add(element);
      Assert.assertEquals(s + 1, list2.size());
      // System.out.println("Added2:"+list2.size());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      lock2.writeLock().unlock();
    }

  }

  public void moveFromList1() {
    while (true) {

      lock1.readLock().lock();
      try {
        // System.out.println("Moving from 2 to 3");
        lock3.writeLock().lock();
        try {
          if (!(list1.size() > 0)) {
            break;
          }
          String obj = (String) list1.remove(0);
          list3.add(obj);
        } finally {
          lock3.writeLock().unlock();
        }
      } finally {
        lock1.readLock().unlock();
      }
    }
  }

  public void moveFromList2() {
    while (true) {

      lock3.writeLock().lock();
      try {
        lock2.readLock().lock();
        try {
          if (!(list2.size() > 0)) {
            break;
          }
          // System.out.println("Moving from 2 to 3");
          String obj = (String) list2.remove(0);
          list3.add(obj);
        } finally {
          lock2.readLock().unlock();
        }
      } finally {
        lock3.writeLock().unlock();
      }
    }
  }

}
