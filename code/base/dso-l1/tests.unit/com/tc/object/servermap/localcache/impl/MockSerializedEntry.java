/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObjectSelfImpl;

public class MockSerializedEntry extends TCObjectSelfImpl {
  private final byte[] myArray;
  private long         lastAccessed;

  public MockSerializedEntry(ObjectID id, byte[] array, TCClass tcClazz) {
    this(id, array, tcClazz, System.currentTimeMillis());
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
}