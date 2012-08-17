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

import junit.framework.Assert;

public class ClusteredConditionTestClient extends ClientBase {
  private static final int RUN_COUNT = 100;

  public ClusteredConditionTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    int index = getBarrierForAllClients().await();
    final ToolkitList<Integer> list = toolkit.getList("test-list", null);
    ToolkitLock lock = toolkit.getLock("lock1");

    try {
      lock.newCondition();
      Assert.fail("newCondition() should have failed");
    } catch (UnsupportedOperationException expected) {
      // ignored
    }

    if (index == 0) {
      //
      for (int i = 0; i < RUN_COUNT; i++) {
        lock.lock();
        try {
          debug("Adding" + i);
          Assert.assertEquals(list.size(), 0);
          list.add(i);
          Assert.assertEquals(list.size(), 1);
          debug("Signalling" + i);
          lock.getCondition().signalAll();
          debug("waiting" + i);
          lock.getCondition().await();
          debug("wait complete" + i);
          Assert.assertEquals(list.size(), 0);
        } finally {
          lock.unlock();
        }
      }
      lock.lock();
      try {
        // kick off the other waiting client
        lock.getCondition().signalAll();
      } finally {
        lock.unlock();
      }

    } else if (index == 1) {
      // Let the other node to add the element and go to wait state.
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          return list.size() > 0;
        }
      });
      Assert.assertEquals(list.size(), 1);
      for (int i = 0; i < RUN_COUNT; i++) {
        lock.lock();
        try {
          debug("Removing" + i);
          Assert.assertEquals(list.size(), 1);
          Assert.assertEquals(list.remove(0).intValue(), i);
          Assert.assertEquals(list.size(), 0);
          debug("Signalling" + i);
          lock.getCondition().signalAll();
          debug("waiting" + i);
          lock.getCondition().await();
          debug("wait complete" + i);
        } finally {
          lock.unlock();
        }
      }
    }
  }
}