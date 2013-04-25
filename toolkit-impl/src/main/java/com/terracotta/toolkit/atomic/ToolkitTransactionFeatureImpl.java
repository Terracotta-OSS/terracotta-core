/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.atomic;

import org.terracotta.toolkit.atomic.ToolkitTransaction;
import org.terracotta.toolkit.atomic.ToolkitTransactionController;
import org.terracotta.toolkit.atomic.ToolkitTransactionType;
import org.terracotta.toolkit.nonstop.NonStopException;

import com.tc.abortable.AbortedOperationException;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.StringLockID;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.feature.EnabledToolkitFeature;

public class ToolkitTransactionFeatureImpl extends EnabledToolkitFeature implements ToolkitTransactionController {
  private static final String   ATOMIC_LOCK_NAME = "atomic-concurrent-lock";
  private static final String   DELIMITER        = "|";
  private final PlatformService platformService;

  public ToolkitTransactionFeatureImpl(PlatformService platformService) {
    this.platformService = platformService;
  }

  @Override
  public ToolkitTransaction beginTransaction(ToolkitTransactionType type) {
    LockID lockID = getLockID(type);
    LockLevel level = getLockLevel(type);
    try {
      platformService.beginAtomicTransaction(lockID, level);
    } catch (AbortedOperationException e) {
      throw new NonStopException("begin timed out", e);
    }
    return new ToolkitTransactionImpl(platformService, lockID, level);
  }

  private LockID getLockID(ToolkitTransactionType type) {
    if (type == ToolkitTransactionType.SYNC) {
      // ATOMIC SYNC_WRITE LOCK ID unique for each thread.
      return new StringLockID(ATOMIC_LOCK_NAME + DELIMITER + platformService.getUUID() + DELIMITER
                              + Thread.currentThread().getId());
    } else {
      return new StringLockID(ATOMIC_LOCK_NAME);
    }
  }

  private LockLevel getLockLevel(ToolkitTransactionType type) {
    if (type == ToolkitTransactionType.SYNC) {
      return LockLevel.SYNCHRONOUS_WRITE;
    } else {
      return LockLevel.CONCURRENT;
    }
  }

}
