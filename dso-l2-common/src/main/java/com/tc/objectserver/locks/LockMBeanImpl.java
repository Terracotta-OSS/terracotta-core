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
package com.tc.objectserver.locks;

import com.tc.object.locks.LockID;

import java.io.Serializable;
import java.util.Arrays;

public class LockMBeanImpl implements LockMBean, Serializable {
  private final LockID                  lockID;
  private final ServerLockContextBean[] contexts;

  public LockMBeanImpl(LockID lockID, ServerLockContextBean[] contexts) {
    this.lockID = lockID;
    this.contexts = contexts;
  }

  @Override
  public ServerLockContextBean[] getContexts() {
    return contexts;
  }

  @Override
  public LockID getLockID() {
    return lockID;
  }

  @Override
  public String toString() {
    return "LockMBeanImpl [contexts=" + Arrays.toString(contexts) + ", lockID=" + lockID + "]";
  }

}
