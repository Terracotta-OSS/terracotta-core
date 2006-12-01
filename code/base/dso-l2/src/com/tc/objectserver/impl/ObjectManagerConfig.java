/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

public class ObjectManagerConfig {

  private final long gcThreadSleepTime;
  private boolean doGC;
  private final boolean verboseGC;
  private final boolean paranoid;
  private final int deleteBatchSize;

  public ObjectManagerConfig(long gcThreadSleepTime, boolean doGC, boolean verboseGC, boolean paranoid, int deleteBatchSize) {
    this.gcThreadSleepTime = gcThreadSleepTime;
    this.doGC = doGC;
    this.verboseGC = verboseGC;
    this.paranoid = paranoid;
    this.deleteBatchSize = deleteBatchSize;
  }

  public boolean paranoid() {
    return paranoid;
  }

  public boolean doGC() {
    return doGC;
  }

  public long gcThreadSleepTime() {
    return gcThreadSleepTime;
  }

  public boolean verboseGC() {
    return verboseGC;
  }

  public int getDeleteBatchSize() {
    return deleteBatchSize;
  }

}
