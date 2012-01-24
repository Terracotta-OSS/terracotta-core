/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test execution modes.
 */
public enum ExecutionMode {
  /**
   * Only production ready tests are run, failure emails sent to everyone.
   */
  PRODUCTION,

  /**
   * Only newly added tests or tests being debugged are run, failure emails sent to infrastructure/QA.
   */
  QUARANTINE,

  /**
   * All tests are run.
   */
  DEVELOPMENT;

  private static final Map<String, ExecutionMode> stringToMode = new HashMap();
  static {
    for (ExecutionMode mode : values()) {
      stringToMode.put(mode.toString().toUpperCase(), mode);
    }
  }

  public static ExecutionMode fromString(String name) {
    if (name == null) {
      name = "";
    }
    name = name.trim().toUpperCase();
    return stringToMode.get(name);
  }
}
