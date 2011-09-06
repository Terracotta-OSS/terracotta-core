/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

public class LockInfoByThreadIDTest extends TCTestCase {

  public void testLockInfoByThreadIDs() {
    LockInfoByThreadID lockInfo = new LockInfoByThreadIDImpl();
    ThreadID[] threadIDs = new ThreadID[] { new ThreadID(100), new ThreadID(200), new ThreadID(300) };
    String[] locks = new String[] { "lock 1", "lock 2", "lock 3", "lock 4" };

    lockInfo.addLock(LockState.HOLDING, threadIDs[0], locks[0]);
    lockInfo.addLock(LockState.HOLDING, threadIDs[0], locks[3]);
    lockInfo.addLock(LockState.WAITING_ON, threadIDs[0], locks[1]);
    lockInfo.addLock(LockState.WAITING_TO, threadIDs[0], locks[2]);

    lockInfo.addLock(LockState.WAITING_ON, threadIDs[1], locks[0]);
    lockInfo.addLock(LockState.WAITING_ON, threadIDs[1], locks[3]);
    lockInfo.addLock(LockState.WAITING_TO, threadIDs[1], locks[1]);

    lockInfo.addLock(LockState.HOLDING, threadIDs[2], locks[1]);
    lockInfo.addLock(LockState.WAITING_TO, threadIDs[2], locks[0]);

    Assert.eval(lockInfo.getHeldLocks(threadIDs[0]).size() == 2);
    Assert.eval(lockInfo.getHeldLocks(threadIDs[0]).contains(locks[0]));
    Assert.eval(lockInfo.getHeldLocks(threadIDs[0]).contains(locks[3]));
    Assert.eval(lockInfo.getWaitOnLocks(threadIDs[0]).size() == 1);
    Assert.eval(lockInfo.getWaitOnLocks(threadIDs[0]).contains(locks[1]));
    Assert.eval(lockInfo.getPendingLocks(threadIDs[0]).size() == 1);
    Assert.eval(lockInfo.getPendingLocks(threadIDs[0]).contains(locks[2]));

    Assert.eval(lockInfo.getHeldLocks(threadIDs[1]).size() == 0);
    Assert.eval(lockInfo.getWaitOnLocks(threadIDs[1]).size() == 2);
    Assert.eval(lockInfo.getWaitOnLocks(threadIDs[1]).contains(locks[0]));
    Assert.eval(lockInfo.getWaitOnLocks(threadIDs[1]).contains(locks[3]));
    Assert.eval(lockInfo.getPendingLocks(threadIDs[1]).size() == 1);
    Assert.eval(lockInfo.getPendingLocks(threadIDs[1]).contains(locks[1]));

    Assert.eval(lockInfo.getHeldLocks(threadIDs[2]).size() == 1);
    Assert.eval(lockInfo.getHeldLocks(threadIDs[2]).contains(locks[1]));
    Assert.eval(lockInfo.getWaitOnLocks(threadIDs[2]).size() == 0);
    Assert.eval(lockInfo.getPendingLocks(threadIDs[2]).size() == 1);
    Assert.eval(lockInfo.getPendingLocks(threadIDs[2]).contains(locks[0]));

  }
}
