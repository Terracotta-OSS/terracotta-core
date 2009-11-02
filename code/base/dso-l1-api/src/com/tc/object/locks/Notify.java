/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCSerializable;

public interface Notify extends TCSerializable {
//
  public boolean isNull();
  
  public LockID getLockID();
  
  public ThreadID getThreadID();
  
  public boolean getIsAll();
}
