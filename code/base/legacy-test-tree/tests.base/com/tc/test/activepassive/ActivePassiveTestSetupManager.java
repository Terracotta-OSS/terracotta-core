/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

public class ActivePassiveTestSetupManager {

  public static final String MUTATE_VALIDATE         = "mutate-validate";
  public static final String CONTINUOUS_ACTIVE_CRASH = "continuous-active-crash";
  public static final String DISK                    = "disk";
  public static final String NETWORK                 = "network";

  // must match strings used in tc-config.xml
  public static final String PERMANENT_STORE         = "permanent-store";
  public static final String TEMPORARY_SWAP_ONLY     = "temporary-swap-only";

  private int                serverCount;
  private String             serverCrashMode;
  private String             serverShareMode;
  private String             persistenceMode;
  private long               waitTimeInSec;

  public void setServerCount(int count) {
    if (count < 2) { throw new AssertionError("Server count must be 2 or more:  count=[" + count + "]"); }
    serverCount = count;
  }

  public int getServerCount() {
    return serverCount;
  }

  public void setServerCrashMode(String mode) {
    if (!mode.equals(MUTATE_VALIDATE) && !mode.equals(CONTINUOUS_ACTIVE_CRASH)) { throw new AssertionError(
                                                                                                           "Unrecognized crash mode ["
                                                                                                               + mode
                                                                                                               + "]"); }
    serverCrashMode = mode;
  }

  public String getServerCrashMode() {
    if (serverCrashMode == null) { throw new AssertionError("Server crash mode was not set."); }
    return serverCrashMode;
  }

  public void setServerShareDataMode(String mode) {
    if (!mode.equals(DISK) && !mode.equals(NETWORK)) { throw new AssertionError("Unrecognized share data mode [" + mode
                                                                                + "]"); }
    serverShareMode = mode;
  }

  public boolean isNetworkShare() {
    if (serverShareMode == null) { throw new AssertionError("Server share mode was not set."); }
    return serverShareMode.equals(NETWORK);
  }

  public void setServerPersistenceMode(String mode) {
    if (!mode.equals(PERMANENT_STORE) && !mode.equals(TEMPORARY_SWAP_ONLY)) { throw new AssertionError(
                                                                                                       "Unrecognized persistence mode ["
                                                                                                           + mode + "]"); }
    persistenceMode = mode;
  }

  public String getServerPersistenceMode() {
    if (persistenceMode == null) { throw new AssertionError("Server persistence mode was not set."); }
    return persistenceMode;
  }

  public void setServerCrashWaitInSec(long time) {
    if (time < 0) { throw new AssertionError("Wait time should not be a negative number."); }
    waitTimeInSec = time;
  }

  public long getWaitTimeInSec() {
    return waitTimeInSec;
  }
}
