/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
