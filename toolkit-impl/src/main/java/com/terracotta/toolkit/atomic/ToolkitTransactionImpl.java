/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
