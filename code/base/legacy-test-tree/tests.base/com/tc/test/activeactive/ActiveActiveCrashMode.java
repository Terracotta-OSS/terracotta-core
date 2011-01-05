/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activeactive;

import com.tc.test.MultipleServersCrashMode;

public class ActiveActiveCrashMode extends MultipleServersCrashMode {

  public ActiveActiveCrashMode(String mode) {
    super(mode);
  }

  @Override
  public void checkMode() {
    if (!mode.equals(CRASH_AFTER_MUTATE) && !mode.equals(CONTINUOUS_ACTIVE_CRASH) && !mode.equals(RANDOM_SERVER_CRASH)
        && !mode.equals(AA_CUSTOMIZED_CRASH) && !mode.equals(NO_CRASH) && !mode.equals(AA_CONTINUOUS_CRASH_ONE)) { throw new AssertionError(
                                                                                                                                            "Unrecognized crash mode ["
                                                                                                                                                + mode
                                                                                                                                                + "]"); }
  }
}
