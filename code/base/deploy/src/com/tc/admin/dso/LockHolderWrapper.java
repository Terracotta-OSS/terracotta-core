/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.LockElementWrapper;
import com.tc.objectserver.lockmanager.api.LockHolder;

public class LockHolderWrapper implements LockElementWrapper {
  private LockHolder lockHolder;
  private String lockId;
  private String stackTrace;

  public LockHolderWrapper(LockHolder lockHolder) {
    this.lockHolder = lockHolder;
    this.lockId = lockHolder.getLockID().asString();
    if(this.lockId.charAt(0) != '@') {
      this.lockId = "^"+this.lockId;
    }
  }

  public String getLockID() { return lockId; }
  public String getLockLevel() { return lockHolder.getLockLevel(); }
  public String getNodeID() { return lockHolder.getNodeID().toString(); }
  public String getChannelAddr() { return lockHolder.getChannelAddr(); }
  public long getTimeAcquired() { return lockHolder.getTimeAcquired();  }
  public long getTimeReleased() { return lockHolder.getTimeReleased(); }
  public long getThreadID() { return lockHolder.getThreadID().toLong(); }
  public long getWaitTimeInMillis() { return lockHolder.getAndSetWaitTimeInMillis(); }
  public long getHeldTimeInMillis() { return lockHolder.getAndSetHeldTimeInMillis(); }
  
  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }
  
  public String getStackTrace() {
    return this.stackTrace;
  }
}


