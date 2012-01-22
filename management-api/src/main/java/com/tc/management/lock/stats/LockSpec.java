/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.object.locks.LockID;

import java.util.Collection;

public interface LockSpec {
  public LockID getLockID();

  public String getObjectType();

  public LockStats getServerStats();

  public LockStats getClientStats();

  public Collection children();

}
