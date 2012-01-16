package com.tc.test.config.model;


public class CrashConfig {
  private long            serverCrashWaitTimeInSec = 15;
  private int             maxCrashCount            = Integer.MAX_VALUE;
  private ServerCrashMode crashMode = ServerCrashMode.NO_CRASH;

  public long getServerCrashWaitTimeInSec() {
    return serverCrashWaitTimeInSec;
  }

  public void setServerCrashWaitTimeInSec(long serverCrashWaitTimeInSec) {
    this.serverCrashWaitTimeInSec = serverCrashWaitTimeInSec;
  }

  public int getMaxCrashCount() {
    return maxCrashCount;
  }

  public void setMaxCrashCount(int maxCrashCount) {
    this.maxCrashCount = maxCrashCount;
  }

  public ServerCrashMode getCrashMode() {
    return crashMode;
  }

  public void setCrashMode(ServerCrashMode crashMode) {
    this.crashMode = crashMode;
  }

}
