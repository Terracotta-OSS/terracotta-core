/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.DebugUtil;

import java.io.Serializable;

public class CacheData implements Serializable {
  private Object              value;
  private long                maxIdleMillis;
  private long                maxTTLMillis;
  private long                createTime;
  private Timestamp           timestamp;                                      // timestamp contains the time when the
                                                                              // CacheData will be expired

  private transient long      lastAccessedTimeInMillis;
  private boolean             invalidated = false;

  public CacheData(Object value, long maxIdleSeconds, long maxTTLSeconds) {
    this.value = value;
    this.createTime = System.currentTimeMillis();
    this.maxIdleMillis = maxIdleSeconds * 1000;
    this.maxTTLMillis = maxTTLSeconds * 1000;
    if (maxIdleMillis <= 0) {
      this.timestamp = new Timestamp(this.createTime + maxTTLMillis, this.createTime + maxTTLMillis);
    } else {
      this.timestamp = new Timestamp(this.createTime + maxIdleMillis, this.createTime + maxTTLMillis);
    }
    this.lastAccessedTimeInMillis = 0;
  }

  public CacheData() {
    this.lastAccessedTimeInMillis = System.currentTimeMillis();
  }

  Timestamp getTimestamp() {
    return timestamp;
  }

  synchronized boolean isValid() {
    if (DebugUtil.DEBUG) {
      System.err.println("Client id: " + ManagerUtil.getClientID() + " Checking if CacheData isValid [invalidated]: "
                         + invalidated + " [lastAccessedTimeInMillis]: " + lastAccessedTimeInMillis + " current millis: " + System.currentTimeMillis());
      
    }
    if (invalidated) { return false; }
    return hasNotExpired() && isStillAlive();
  }

  private boolean hasNotExpired() {
    if (DebugUtil.DEBUG) {
      System.err.println("Client id: " + ManagerUtil.getClientID() + " getIdleMillis: " + getIdleMillis());
    }
    if (getMaxInactiveMillis() <= 0) { return true; }
    return getIdleMillis() < getMaxInactiveMillis();
  }

  private boolean isStillAlive() {
    if (maxTTLMillis <= 0) { return true; }
    return System.currentTimeMillis() <= getTimeToDieMillis();
  }

  private long getTimeToDieMillis() {
    return this.createTime + maxTTLMillis;
  }

  synchronized long getIdleMillis() {
    if (lastAccessedTimeInMillis == 0) return 0;
    return Math.max(System.currentTimeMillis() - lastAccessedTimeInMillis, 0);
  }

  synchronized void finish() {
    lastAccessedTimeInMillis = System.currentTimeMillis();
  }

  synchronized void accessed() {
    lastAccessedTimeInMillis = System.currentTimeMillis();
    if (DebugUtil.DEBUG) {
      System.err.println("Client " + ManagerUtil.getClientID() + " accessing " + lastAccessedTimeInMillis + " current millis: " + System.currentTimeMillis());
      Thread.dumpStack();
    }
  }

  synchronized long getMaxInactiveMillis() {
    return maxIdleMillis;
  }

  public synchronized Object getValue() {
    return value;
  }

  synchronized void invalidate() {
    this.invalidated = true;
  }

  synchronized boolean isInvalidated() {
    return this.invalidated;
  }

  public int hashCode() {
    return this.value.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof CacheData)) { return false; }
    CacheData cd = (CacheData) obj;
    return this.value.equals(cd.value) && (this.maxIdleMillis == cd.maxIdleMillis)
           && (this.maxTTLMillis == cd.maxTTLMillis);
  }

}
