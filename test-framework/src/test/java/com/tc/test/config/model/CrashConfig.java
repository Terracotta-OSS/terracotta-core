package com.tc.test.config.model;

/**
 * Controls the crashing of the servers in a test
 * @author rsingh
 */
public class CrashConfig {
  private long            serverCrashWaitTimeInSec = 15;
  private int             maxCrashCount            = Integer.MAX_VALUE;
  private ServerCrashMode crashMode = ServerCrashMode.NO_CRASH;

  /**
   * Wait time in seconds before a server is crahsed in a group 
   * @return
   */
  public long getServerCrashWaitTimeInSec() {
    return serverCrashWaitTimeInSec;
  }

  /**
   * Sets the wait time before crashing a server
   * @param serverCrashWaitTimeInSec time in seconds to wait before a server is crashed
   */
  public void setServerCrashWaitTimeInSec(long serverCrashWaitTimeInSec) {
    this.serverCrashWaitTimeInSec = serverCrashWaitTimeInSec;
  }

  /**
   * Maximum number of server crashes allowed in the test, Default Unlimited
   */
  public int getMaxCrashCount() {
    return maxCrashCount;
  }

  /**
   * Sets the maximum number of server crashes in the test
   */
  public void setMaxCrashCount(int maxCrashCount) {
    this.maxCrashCount = maxCrashCount;
  }

  /**
   * returns the server crash mode, Default is NO_CRASH
   */
  public ServerCrashMode getCrashMode() {
    return crashMode;
  }

  /**
   * Sets the server crash mode
   */
  public void setCrashMode(ServerCrashMode crashMode) {
    this.crashMode = crashMode;
  }

}
