/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObjectSelfImpl;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MockSerializedEntry extends TCObjectSelfImpl {
  private final AtomicInteger touchCount = new AtomicInteger(0);
  private final byte[]        myArray;
  private long                lastAccessed;

  public MockSerializedEntry(ObjectID id, byte[] array, TCClass tcClazz) {
    this(id, array, tcClazz, System.currentTimeMillis());
  }

  @Override
  public int touch() {
    return touchCount.incrementAndGet();
  }

  @Override
  public int untouch() {
    return touchCount.decrementAndGet();
  }

  public MockSerializedEntry(ObjectID id, byte[] array, TCClass tcClazz, long lastAccessedTime) {
    this.initializeTCObject(id, tcClazz, false);
    this.myArray = array;
    this.lastAccessed = lastAccessedTime;
  }

  public synchronized void setLastAccessedTime(long time) {
    this.lastAccessed = time;
  }

  public synchronized long getLastAccessedTime() {
    return lastAccessed;
  }

  public synchronized byte[] getSerializedBytes() {
    return this.myArray;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (lastAccessed ^ (lastAccessed >>> 32));
    result = prime * result + Arrays.hashCode(myArray);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MockSerializedEntry other = (MockSerializedEntry) obj;
    if (!Arrays.equals(myArray, other.myArray)) return false;
    return true;
  }
}