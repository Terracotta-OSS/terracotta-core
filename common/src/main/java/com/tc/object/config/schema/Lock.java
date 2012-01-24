/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
