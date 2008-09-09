/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public class MultipleServersSharedDataMode {
  public static final String DISK    = "disk";
  public static final String NETWORK = "network";

  private final String       mode;

  public MultipleServersSharedDataMode(String mode) {
    if (!mode.equals(DISK) && !mode.equals(NETWORK)) { throw new AssertionError("Unrecognized share data mode [" + mode
                                                                                + "]"); }
    this.mode = mode;
  }

  public String getMode() {
    if (mode == null) { throw new AssertionError("Mode was not set"); }
    return mode;
  }

  public boolean isNetworkShare() {
    return getMode().equals(NETWORK);
  }
}
