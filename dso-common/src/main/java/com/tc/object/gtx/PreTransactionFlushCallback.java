/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;

public interface PreTransactionFlushCallback {

  void preTransactionFlush(final LockID lockID, ServerLockLevel level);

}
