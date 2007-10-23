/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.LockElementWrapper;
import com.tc.management.L2LockStatsManagerImpl.LockStat;

import java.util.HashMap;

public class LockStatWrapper implements LockElementWrapper {
  private LockStat                            lockStat;
  private String                              lockId;

  private static HashMap<String, TraceHolder> traceCache = new HashMap<String, TraceHolder>();

  static class TraceHolder {
    private String stackTrace;
    private String allStackTraces;

    TraceHolder(String stackTrace, String allStackTraces) {
      this.stackTrace = stackTrace;
      this.allStackTraces = allStackTraces;
    }
  }

  public LockStatWrapper(LockStat lockStat) {
    this.lockStat = lockStat;
    this.lockId = lockStat.getLockID().asString();
    if (this.lockId.charAt(0) != '@' && this.lockId.charAt(0) != '^') {
      this.lockId = "^" + this.lockId;
    }
  }

  public String getLockID() {
    return lockId;
  }

  public long getNumOfLockRequested() {
    return lockStat.getNumOfLockRequested();
  }

  public long getNumOfLockReleased() {
    return lockStat.getNumOfLockReleased();
  }

  public long getNumOfPendingRequests() {
    return lockStat.getNumOfPendingRequests();
  }

  public long getNumOfPendingWaiters() {
    return lockStat.getNumOfPendingWaiters();
  }

  public long getNumOfLockHopRequests() {
    return lockStat.getNumOfLockHopRequests();
  }

  public long getAvgWaitTimeInMillis() {
    return lockStat.getAvgWaitTimeInMillis();
  }

  public long getAvgHeldTimeInMillis() {
    return lockStat.getAvgHeldTimeInMillis();
  }

  public void setStackTrace(String stackTrace) {
    if (stackTrace != null) {
      TraceHolder holder = traceCache.get(lockId);
      if (holder == null) {
        traceCache.put(lockId, new TraceHolder(stackTrace, null));
      }
    } else {
      traceCache.remove(lockId);
    }
  }

  public String getStackTrace() {
    TraceHolder holder = traceCache.get(lockId);
    return holder != null ? holder.stackTrace : null;
  }

  public void setAllStackTraces(String allStackTraces) {
    if (allStackTraces != null) {
      TraceHolder holder = traceCache.get(lockId);
      if (holder == null) {
        traceCache.put(lockId, new TraceHolder(null, allStackTraces));
      }
    } else {
      traceCache.remove(lockId);
    }
  }

  public String getAllStackTraces() {
    TraceHolder holder = traceCache.get(lockId);
    return holder != null ? holder.allStackTraces : null;
  }
}
