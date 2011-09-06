/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.LockID;

public interface LockFlushCallback {

  /* This method is called when all the transactions made under that Lock are ACKED */
  public void transactionsForLockFlushed(LockID id);
}
