/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

public interface LockDefinition {

  public static final String TC_AUTOLOCK_NAME = "tc:autolock";

  public void setLockName(String lockName);

  public String getLockName();

  public void setLockLevel(ConfigLockLevel lt);

  public ConfigLockLevel getLockLevel();

  public int getLockLevelAsInt();

  public boolean isAutolock();

  public void commit();

}
