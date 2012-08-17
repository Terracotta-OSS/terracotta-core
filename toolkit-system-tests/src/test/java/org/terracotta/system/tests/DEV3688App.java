/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

public class DEV3688App extends ClientBase {
  private final List<Integer> sharedList;
  private final ReadWriteLock lock;

  public DEV3688App(String[] args) {
    super(args);
    this.sharedList = getClusteringToolkit().getList("sharedList", null);
    this.lock = getClusteringToolkit().getReadWriteLock("testLock");
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    // do initialization to the array -- add some 1000 numbers and wait for 2nd client to come up
    lock.writeLock().lock();
    try {
      int length = this.sharedList.size();
      for (int i = length; i < 1000 + length; i++) {
        if (i % 100 == 0) {
          System.out.println("Adding " + i);
        }
        this.sharedList.add(new Integer(i));
      }
    } finally {
      lock.writeLock().unlock();
    }

    waitForAllClients();
  }

}
