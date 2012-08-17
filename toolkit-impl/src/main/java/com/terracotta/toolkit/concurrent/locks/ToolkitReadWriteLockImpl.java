/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

public class ToolkitReadWriteLockImpl extends UnnamedToolkitReadWriteLock {
  private static final String PREFIX = "_tc_read_write_lock@";
  private final String        name;

  public ToolkitReadWriteLockImpl(String name) {
    super(generateLockId(name));
    this.name = name;
  }

  private static String generateLockId(String id) {
    return PREFIX + id;
  }

  @Override
  public String getName() {
    return name;
  }
}
