/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.concurrent.locks;

public interface LockStrategy {

  Object generateLockIdForKey(Object key);
}
