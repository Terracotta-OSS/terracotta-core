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
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.object.locks.LockLevel;

// package protected class
class ToolkitLockDetail {

  private final Object                  lockId;
  private final ToolkitLockTypeInternal lockType;

  public static ToolkitLockDetail newLockDetail(String stringLockId, ToolkitLockTypeInternal lockType) {
    return new ToolkitLockDetail(stringLockId, lockType);
  }

  public static ToolkitLockDetail newLockDetail(long longLockId, ToolkitLockTypeInternal lockType) {
    return new ToolkitLockDetail(longLockId, lockType);
  }

  private ToolkitLockDetail(Object lockId, ToolkitLockTypeInternal lockType) {
    this.lockId = lockId;
    this.lockType = lockType;
  }

  public Object getLockId() {
    return lockId;
  }

  public ToolkitLockTypeInternal getToolkitInternalLockType() {
    return lockType;
  }

  public LockLevel getLockLevel() {
    return LockingUtils.translate(lockType);
  }

}
