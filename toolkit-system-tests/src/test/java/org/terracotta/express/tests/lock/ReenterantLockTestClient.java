/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.util.concurrent.Callable;

public class ReenterantLockTestClient extends ClientBase {

  public ReenterantLockTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    ToolkitLock lock = toolkit.getLock("reenterant");
    int index = getBarrierForAllClients().await();
    final ToolkitList list = toolkit.getList("samplelist", null);
    if (index == 0) {
      lockTwice(lock);
      try {
        list.add("hello");
        lock.getCondition().await();
      } finally {
        unLockTwice(lock);
      }
    } else if (index == 1) {
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          return list.size() > 0;
        }
      });
      lockTwice(lock);
      try {
        lock.getCondition().signalAll();
      } finally {
        unLockTwice(lock);
      }
    }
  }

  private void lockTwice(ToolkitLock lock) {
    lock.lock();
    lock.lock();

  }

  private void unLockTwice(ToolkitLock lock) {
    lock.unlock();
    lock.unlock();

  }

}