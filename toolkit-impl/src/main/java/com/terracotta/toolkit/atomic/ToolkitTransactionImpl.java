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
package com.terracotta.toolkit.atomic;

import org.terracotta.toolkit.atomic.ToolkitTransaction;
import org.terracotta.toolkit.nonstop.NonStopException;

import com.tc.abortable.AbortedOperationException;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.platform.PlatformService;

public class ToolkitTransactionImpl implements ToolkitTransaction {
  private final PlatformService platformService;
  private final LockID          lockID;
  private final LockLevel       level;

  public ToolkitTransactionImpl(PlatformService platformService, LockID lockID, LockLevel level) {
    this.platformService = platformService;
    this.lockID = lockID;
    this.level = level;
  }

  @Override
  public void commit() {
    try {
      platformService.commitAtomicTransaction(lockID, level);
    } catch (AbortedOperationException e) {
      throw new NonStopException("commit timed out", e);
    }
  }

}
