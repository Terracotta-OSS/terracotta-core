/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

public class ToolkitLockImpl extends UnnamedToolkitLock {
  private static final String PREFIX = "_tclock@";
  private final String        lockName;

  public ToolkitLockImpl(String name, ToolkitLockTypeInternal lockType) {
    super(generateLockID(name), lockType);
    this.lockName = name;
  }

  private static String generateLockID(String name) {
    return PREFIX + name;
  }

  @Override
  public String getName() {
    return lockName;
  }

}