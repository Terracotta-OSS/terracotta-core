/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public abstract class MultipleServersCrashMode {

  public static final String NO_CRASH                = "no-crash";
  public static final String CRASH_AFTER_MUTATE      = "crash-after-mutate";
  public static final String CONTINUOUS_ACTIVE_CRASH = "continuous-active-crash";
  public static final String RANDOM_SERVER_CRASH     = "random-server-crash";
  public static final String AP_CUSTOMIZED_CRASH     = "active-passive-customized-crash";
  public static final String AA_CUSTOMIZED_CRASH     = "active-active-customized-crash";
  // only crash active-active group-0
  public static final String AA_CONTINUOUS_CRASH_ONE = "active-active-continuous-crash-one";

  protected String           mode;

  protected MultipleServersCrashMode(String mode) {
    this.mode = mode;
    checkMode();
  }

  public String getMode() {
    if (mode == null) { throw new AssertionError("Mode was not set"); }
    return mode;
  }

  public abstract void checkMode();
}
