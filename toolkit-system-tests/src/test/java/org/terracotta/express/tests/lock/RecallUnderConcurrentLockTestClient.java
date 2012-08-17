/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.util.concurrent.ThreadUtil;

public class RecallUnderConcurrentLockTestClient extends ClientBase {

  public RecallUnderConcurrentLockTestClient(String[] args) {
    super(args);
  }

  private static final String LOCK_STRING = "lock";

  @Override
  public void test(Toolkit toolkit) throws Exception {
    int nodeId = getBarrierForAllClients().await();
    ToolkitLock write = ((ToolkitInternal) toolkit).getLock(LOCK_STRING, ToolkitLockTypeInternal.WRITE);
    ToolkitLock read = ((ToolkitInternal) toolkit).getLock(LOCK_STRING, ToolkitLockTypeInternal.READ);
    ToolkitLock concurrent = ((ToolkitInternal) toolkit).getLock(LOCK_STRING, ToolkitLockTypeInternal.CONCURRENT);

    if (nodeId == 0) {
      // Grab the greedy lock
      write.lock();
      write.unlock();

      concurrent.lock();
      try {
        getBarrierForAllClients().await();
        // other node tries to take the lock here
        ThreadUtil.reallySleep(5000);
      } finally {
        // Unlock before awaiting
        concurrent.unlock();
      }
      getBarrierForAllClients().await();
    } else {
      getBarrierForAllClients().await();
      read.lock();
      try {
        getBarrierForAllClients().await();
      } finally {
        read.unlock();
      }
    }

    getBarrierForAllClients().await();
  }
}
