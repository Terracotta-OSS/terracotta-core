/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.locks.ReadWriteLock;

import junit.framework.Assert;

public class RecalledLockNeverRecalledTestClient extends ClientBase {

  private ReadWriteLock       lock;
  private ToolkitAtomicLong nodeOneFinished;
  private ToolkitBarrier    barrierForTwo;

  public RecalledLockNeverRecalledTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    long gcTime = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L1_LOCKMANAGER_TIMEOUT_INTERVAL);
    lock = toolkit.getReadWriteLock("sameReadWriteLock");
    nodeOneFinished = toolkit.getAtomicLong("Node1Finished");
    barrierForTwo = toolkit.getBarrier("barrierfortwo", 2);
    Assert.assertEquals(5000, gcTime);
    int index = getBarrierForAllClients().await();
    switch (index) {
      case 0:
        node0();
        break;
      case 1:
        node1();
        break;
      case 2:
        node2();
        break;
      default:
        throw new AssertionError("Node count should be 3 !! : idx  = " + index);
    }
  }

  private void node2() throws Exception {
    barrierForTwo.await();
  }

  /**
   * The purpose of this Node is to grab a READ lock greedily and then use it for sometime and then never use it so it
   * gets recalled.
   */
  private void node0() throws Exception {
    String nodeName = "Node 0";
    lock(nodeName); // GET Greedy lock
    unlock(nodeName);

    // Now don't use this lock for 30 seconds so Lock GC kicks in
    log("Node 0 : Sleeping for 30 secs ");
    ThreadUtil.reallySleep(30000);

    do {
      log("Node 0 : Asking for READ lock again");
      lock.readLock().lock(); // Request Lock, this wait in the server first time, then it should be greedy
      try {
        log("Node 0 : Got READ lock again");
        ThreadUtil.reallySleep(3000);
      } finally {
        lock.readLock().unlock();
      }
    } while (nodeOneFinished.intValue() == 0);

    barrierForTwo.await();
    log("Node 0: Exiting");
  }

  private void lock(String nodeName) {
    log(nodeName + ": Asking for READ lock ");
    lock.readLock().lock();
  }

  private void unlock(String nodeName) {
    log(nodeName + ": Releasing READ lock ");
    lock.readLock().unlock();
  }

  /**
   * The purpose of this Node is to grab a READ lock and
   * 
   * @throws Exception
   */
  private void node1() throws Exception {

    String nodeName = "Node 1";
    lock(nodeName); // GET Greedy lock
    try {
      // Just sleep holding it
      log("Node 1 : Sleeping for 60 secs ");
      ThreadUtil.reallySleep(60000);
      // wait for node3 to complete and exit now.
      barrierForTwo.await();
      // Just sleep holding it
      log("Node 1 : Sleeping for 10 secs ");
      ThreadUtil.reallySleep(10000);
    } finally {
      // Now unlock it so that the recallCommit is called.
      unlock(nodeName);
    }

    // Because of a bug in the Server lock manager, the should never be granted.
    lock(nodeName);
    unlock(nodeName);
    nodeOneFinished.incrementAndGet();
    barrierForTwo.await();
    log("Node 1: Exiting");
  }

  private void log(String msg) {
    System.out.println(Thread.currentThread() + msg);
  }

}