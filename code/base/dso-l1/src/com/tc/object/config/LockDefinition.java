/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.builder.ToStringBuilder;

public class LockDefinition {

  public static final String TC_AUTOLOCK_NAME = "tc:autolock";

  private String             lockName         = TC_AUTOLOCK_NAME;
  private ConfigLockLevel    lockLevel;

  private boolean            isCommitted      = false;
  private boolean            autolock;

  public LockDefinition() {
    return;
  }

  public LockDefinition(String lockName, ConfigLockLevel lockLevel) {
    setLockName(lockName);
    setLockLevel(lockLevel);
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public void setLockName(String lockName) {
    commitWriteCheck();
    this.lockName = lockName;
  }

  public String getLockName() {
    commitReadCheck();
    return this.lockName;
  }

  public void setLockLevel(ConfigLockLevel lt) {
    commitWriteCheck();
    this.lockLevel = lt;
  }

  public ConfigLockLevel getLockLevel() {
    commitReadCheck();
    return this.lockLevel;
  }

  public int getLockLevelAsInt() {
    return getLockLevel().getLevel();
  }

  public boolean isAutolock() {
    commitReadCheck();
    return autolock;
  }

  /**
   * Creates the serialized lock name which is the lock name encoded with the lock type.
   */
  public synchronized void commit() {
    if (!isCommitted) {
      normalizeLockName();
      this.autolock = TC_AUTOLOCK_NAME.equals(this.lockName);
    }
    isCommitted = true;
  }

  public boolean equals(Object o) {
    if (o == null) return false;
    if (!(o instanceof LockDefinition)) return false;
    LockDefinition compare = (LockDefinition) o;
    if (lockName == null && compare.lockName != null) return false;
    if (lockLevel == null && compare.lockLevel != null) return false;
    return lockName.equals(compare.lockName) && lockLevel.equals(compare.lockLevel);
  }

  public int hashCode() {
    if (lockName == null || lockLevel == null) return 1;
    return lockName.hashCode() | lockLevel.toString().hashCode();
  }

  private void normalizeLockName() {
    // Strip all white space.
    if (this.lockName != null) this.lockName = this.lockName.replaceAll("\\s*", "");
  }

  private synchronized void commitWriteCheck() {
    if (isCommitted) throw new IllegalStateException("Attempt to alter the state of LockDefinition: " + this
                                                     + " after committing");
  }

  private synchronized void commitReadCheck() {
    if (!isCommitted) throw new IllegalStateException("Attempt to read an uncommitted LockDefinition: " + this);
  }

}