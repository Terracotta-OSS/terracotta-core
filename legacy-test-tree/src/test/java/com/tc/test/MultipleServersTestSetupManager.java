/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public abstract class MultipleServersTestSetupManager {

  private int                            serverCount;
  private long                           serverCrashWaitTimeInSec = 15;
  private int                            maxCrashCount            = Integer.MAX_VALUE;
  private MultipleServersSharedDataMode  serversSharedDataMode;
  private MultipleServersPersistenceMode persistenceMode;
  protected MultipleServersCrashMode     crashMode;
  protected int                          electionTime             = 5;
  protected int                          reconnectWindow          = 120;

  public void setServerCount(int count) {
    serverCount = count;
  }

  public void setServerCrashMode(MultipleServersCrashMode crashMode) {
    this.crashMode = crashMode;
  }

  public int getServerCount() {
    return serverCount;
  }

  public abstract void setServerCrashMode(String mode);

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
    serversSharedDataMode = new MultipleServersSharedDataMode(mode);
  }

  public String getServerSharedDataMode() {
    return serversSharedDataMode.getMode();
  }

  public boolean isNetworkShare() {
    if (serversSharedDataMode == null) { throw new AssertionError("Server share mode was not set."); }
    return serversSharedDataMode.isNetworkShare();
  }

  public void setServerPersistenceMode(String mode) {
    persistenceMode = new MultipleServersPersistenceMode(mode);
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

  public void setElectionTime(int time) {
    this.electionTime = time;
  }

  public int getElectionTime() {
    return this.electionTime;
  }

  public void setReconnectWindow(int time) {
    this.reconnectWindow = time;
  }

  public int getReconnectWindow() {
    return this.reconnectWindow;
  }

  public abstract int getActiveServerGroupCount();

  public abstract int getGroupMemberCount(int groupIndex);

  public abstract int getGroupElectionTime(int groupIndex);

  public abstract String getGroupName(int groupIndex);

  public abstract String getGroupServerShareDataMode(int groupIndex);
}
