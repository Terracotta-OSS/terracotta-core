/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStore;

/**
 * This interface is needed so that we can create a Proxy Object for ToolkitCacheImpl.
 */
public interface ToolkitCacheImplInterface<K, V> extends ToolkitStore<K, V>, ToolkitCacheInternal<K, V> {
  // No additional methods
}
