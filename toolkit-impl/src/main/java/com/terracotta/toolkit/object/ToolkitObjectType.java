/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.store.ToolkitStore;

/**
 * Type of toolkit objects
 */
public enum ToolkitObjectType {

  /**
   * {@link ToolkitList}
   */
  LIST,
  /**
   * {@link ToolkitMap}
   */
  MAP,
  /**
   * {@link ToolkitMap}
   */
  SORTED_MAP,
  /**
   * {@link ToolkitStore}
   */
  STORE,
  /**
   * {@link ToolkitCache}
   */
  CACHE,
  /**
   * {@link ToolkitBlockingQueue}
   */
  BLOCKING_QUEUE,
  /**
   * {@link ToolkitLock}
   */
  LOCK,
  /**
   * {@link ToolkitReadWriteLock}
   */
  READ_WRITE_LOCK,
  /**
   * {@link ToolkitNotifier}
   */
  NOTIFIER,
  /**
   * {@link ToolkitAtomicLong}
   */
  ATOMIC_LONG,
  /**
   * {@link ToolkitBarrier}
   */
  BARRIER,
  /**
   * {@link ToolkitSortedSet}
   */
  SORTED_SET,
  /**
   * {@link ToolkitSet}
   */
  SET;

}
