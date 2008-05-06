/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.DebugUtil;

import java.util.Date;

/**
 * Tracks timestamp information on when the idle and TTL timers for a CacheData value will expire. The TTL timer
 * (timeToDie) cannot be reset, but may not be active. The idle timer is reset each time the item is used.
 */
public class Timestamp {
  private long                  timeToExpireMillis;                                      // Guarded by synchronized
                                                                                          // lock on "this"
  private final long            timeToDieMillis;

  public Timestamp(long createTime, long maxIdleMillis, long maxTTLMillis) {
    this.timeToDieMillis = createTime + maxTTLMillis;
    if (maxIdleMillis <= 0) {
      this.timeToExpireMillis = createTime + maxTTLMillis;
    } else {
      this.timeToExpireMillis = createTime + maxIdleMillis;
    }
    
    if (DebugUtil.DEBUG) {
      System.err.println("Client " + ManagerUtil.getClientID() + " creating timestamp -- maxIdleMillis: " + maxIdleMillis + ", maxTTLMillis: " + maxTTLMillis + ", createTime: " + createTime);
    }
  }

  /**
   * Get the time at which this timestamp will become invalid
   */
  public synchronized long getInvalidatedTimeMillis() {
    if (DebugUtil.DEBUG) {
      System.err.println("Client " + ManagerUtil.getClientID() + " getInvalidatedTimeMillis -- timeToExpireMillis: " + timeToExpireMillis + ", timeToDieMillis: " + timeToDieMillis + ", current time: " + System.currentTimeMillis());
    }
    if (timeToDieMillis <= 0) { 
      return timeToExpireMillis; 
    }
    
    return (timeToExpireMillis < timeToDieMillis)? timeToExpireMillis : timeToDieMillis;
  }

  /**
   * Get the maximum time at which this timestamp can be valid, regardless of usage.
   */
  public synchronized long getExpiredTimeMillis() {
    return timeToExpireMillis;
  }

  /**
   * Modify time at which this timestamp will expire due to idle. The max TTL limit is not affected.
   */
  public synchronized void setExpiredTimeMillis(long millis) {
    if (timeToDieMillis <= 0 || timeToDieMillis > millis) {
      this.timeToExpireMillis = millis;
    }
  }

  public String toString() {
    return new Date(getInvalidatedTimeMillis()).toString();
  }
}
