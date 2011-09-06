/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

/**
 * Represents a lock, from the config file.
 */
public interface Lock {
  
  boolean isAutoLock();
  
  String lockName();
  
  String methodExpression();
  
  LockLevel lockLevel();

}
