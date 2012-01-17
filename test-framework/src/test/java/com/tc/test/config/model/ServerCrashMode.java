/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.config.model;

public enum ServerCrashMode {

  NO_CRASH("no-crash"),
  RANDOM_ACTIVE_CRASH("random-active-crash"),
  RANDOM_SERVER_CRASH("random-server-crash"),
  CUSTOMIZED_CRASH("customized-crash");

  private String mode;

  private ServerCrashMode(String mode) {
    this.mode = mode;
  }

  public String getMode() {
    if (mode == null) { throw new AssertionError("Mode was not set"); }
    return mode;
  }

}
