/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

public class ObjectManagerConfig {

  private final long    gcThreadSleepTime;
  private final boolean doGC;
  private final boolean verboseGC;
  private final boolean paranoid;

  public ObjectManagerConfig(final long gcThreadSleepTime, final boolean doGC, final boolean verboseGC,
                             final boolean paranoid ) {
    this.gcThreadSleepTime = gcThreadSleepTime;
    this.doGC = doGC;
    this.verboseGC = verboseGC;
    this.paranoid = paranoid;
  }

  public boolean paranoid() {
    return this.paranoid;
  }

  public boolean doGC() {
    return this.doGC;
  }

  public long gcThreadSleepTime() {
    return this.gcThreadSleepTime;
  }

  public boolean verboseGC() {
    return this.verboseGC;
  }

  public boolean startGCThread() {
    return (doGC() && gcThreadSleepTime() > 0);
  }

}
