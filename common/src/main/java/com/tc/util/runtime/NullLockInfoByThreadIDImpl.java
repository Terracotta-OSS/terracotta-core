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

import java.util.ArrayList;

public class NullLockInfoByThreadIDImpl implements LockInfoByThreadID {

  @Override
  public ArrayList getHeldLocks(ThreadID threadID) {
    return new ArrayList();
  }

  @Override
  public ArrayList getPendingLocks(ThreadID threadID) {
    return new ArrayList();
  }

  @Override
  public ArrayList getWaitOnLocks(ThreadID threadID) {
    return new ArrayList();
  }

  @Override
  public void addLock(LockState lockState, ThreadID threadID, String lockID) {
    //
  }
}
