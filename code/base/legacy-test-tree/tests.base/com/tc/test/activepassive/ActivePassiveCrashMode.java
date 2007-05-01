/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

public class ActivePassiveCrashMode {
  public static final String CRASH_AFTER_MUTATE      = "crash-after-mutate";
  public static final String CONTINUOUS_ACTIVE_CRASH = "continuous-active-crash";
  public static final String RANDOM_SERVER_CRASH     = "random-server-crash";

  private final String       mode;

  public ActivePassiveCrashMode(String mode) {
    if (!mode.equals(CRASH_AFTER_MUTATE) && !mode.equals(CONTINUOUS_ACTIVE_CRASH) && !mode.equals(RANDOM_SERVER_CRASH)) { throw new AssertionError(
                                                                                                                                                   "Unrecognized crash mode ["
                                                                                                                                                       + mode
                                                                                                                                                       + "]"); }
    this.mode = mode;
  }

  public String getMode() {
    if (mode == null) { throw new AssertionError("Mode was not set"); }
    return mode;
  }
}
