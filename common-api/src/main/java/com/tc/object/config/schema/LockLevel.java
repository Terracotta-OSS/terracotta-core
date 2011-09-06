/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.util.Assert;

/**
 * Represents the level of a lock.
 */
public class LockLevel {

  private final String level;

  private LockLevel(String level) {
    Assert.assertNotBlank(level);

    this.level = level;
  }

  public static final LockLevel WRITE             = new LockLevel("write");
  public static final LockLevel READ              = new LockLevel("read");
  public static final LockLevel CONCURRENT        = new LockLevel("concurrent");
  public static final LockLevel SYNCHRONOUS_WRITE = new LockLevel("synchronous-write");

  public boolean equals(Object that) {
    if (!(that instanceof LockLevel)) return false;
    return ((LockLevel) that).level.equals(this.level);
  }

  public int hashCode() {
    return this.level.hashCode();
  }

  public String toString() {
    return level;
  }

}
