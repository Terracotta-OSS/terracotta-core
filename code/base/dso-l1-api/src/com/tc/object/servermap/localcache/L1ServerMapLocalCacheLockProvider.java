/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface L1ServerMapLocalCacheLockProvider {
  public ReentrantReadWriteLock getLock(Object key);

  public Collection<ReentrantReadWriteLock> getAllLocks();
}
