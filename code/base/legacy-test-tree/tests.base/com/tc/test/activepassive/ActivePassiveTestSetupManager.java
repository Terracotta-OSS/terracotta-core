/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

public class ActivePassiveTestSetupManager {

  private int                          serverCount;
  private long                         serverCrashWaitTimeInSec = 15;
  private int                          maxCrashCount            = Integer.MAX_VALUE;
  private ActivePassiveSharedDataMode  activePassiveMode;
  private ActivePassivePersistenceMode persistenceMode;
  private ActivePassiveCrashMode       crashMode;

  public void setServerCount(int count) {
    if (count < 2) { throw new AssertionError("Server count must be 2 or more:  count=[" + count + "]"); }
    serverCount = count;
  }

  public int getServerCount() {
    return serverCount;
  }

  public void setServerCrashMode(String mode) {
    crashMode = new ActivePassiveCrashMode(mode);
  }

  public void setMaxCrashCount(int count) {
    if (count < 0) { throw new AssertionError("Max crash count should not be a neg number"); }
    maxCrashCount = count;
  }

  public int getMaxCrashCount() {
    return maxCrashCount;
  }

  public String getServerCrashMode() {
    if (crashMode == null) { throw new AssertionError("Server crash mode was not set."); }
    return crashMode.getMode();
  }

  public void setServerShareDataMode(String mode) {
    activePassiveMode = new ActivePassiveSharedDataMode(mode);
  }

  public boolean isNetworkShare() {
    if (activePassiveMode == null) { throw new AssertionError("Server share mode was not set."); }
    return activePassiveMode.isNetworkShare();
  }

  public void setServerPersistenceMode(String mode) {
    persistenceMode = new ActivePassivePersistenceMode(mode);
  }

  public String getServerPersistenceMode() {
    if (persistenceMode == null) { throw new AssertionError("Server persistence mode was not set."); }
    return persistenceMode.getMode();
  }

  public void setServerCrashWaitTimeInSec(long time) {
    if (time < 0) { throw new AssertionError("Wait time should not be a negative number."); }
    serverCrashWaitTimeInSec = time;
  }

  public long getServerCrashWaitTimeInSec() {
    return serverCrashWaitTimeInSec;
  }

}
