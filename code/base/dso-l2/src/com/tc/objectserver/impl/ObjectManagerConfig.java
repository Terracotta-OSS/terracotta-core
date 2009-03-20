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
  private final boolean youngGenGCEnabled;
  private final long    youngGenGCFrequency;

  public ObjectManagerConfig(long gcThreadSleepTime, boolean doGC, boolean verboseGC, boolean paranoid,
                             boolean youngGenGCEnabled, long youngGenGCFrequency) {
    this.gcThreadSleepTime = gcThreadSleepTime;
    this.doGC = doGC;
    this.verboseGC = verboseGC;
    this.paranoid = paranoid;
    this.youngGenGCEnabled = youngGenGCEnabled;
    this.youngGenGCFrequency = youngGenGCFrequency;
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

  public boolean isYoungGenDGCEnabled() {
    return this.youngGenGCEnabled;
  }

  public long getYoungGenDGCFrequencyInMillis() {
    return this.youngGenGCFrequency;
  }

  public boolean startGCThread() {
    return ((doGC() && gcThreadSleepTime() > 0) || (isYoungGenDGCEnabled() && getYoungGenDGCFrequencyInMillis() > 0));

  }

}
