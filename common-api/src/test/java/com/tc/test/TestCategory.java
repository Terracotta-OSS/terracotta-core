/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of possible test categories.
 */
public enum TestCategory {
  /**
   * A production test that has been vetted by QA.
   */
  PRODUCTION,

  /**
   * A test that has been broken and is quarantined.
   */
  QUARANTINED,

  /**
   * A test that is new and has not yet been vetted by QA.
   */
  TRIAGED,

  /**
   * A test that has not been categorized. In the current monkey staging process, uncategorized tests are treated as
   * triaged.
   */
  UNCATEGORIZED;

  private static final Map<String, TestCategory> stringToCategory = new HashMap();
  static {
    for (TestCategory category : values()) {
      stringToCategory.put(category.toString().toUpperCase(), category);
    }
  }

  public static TestCategory fromString(String name) {
    if (name == null) {
      name = "";
    }
    name = name.trim().toUpperCase();
    return stringToCategory.get(name);
  }
}
