/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.meta;

/**
 * A callback for {@link ToolkitCacheWithMetadata}
 */
public interface ToolkitCacheMetaDataCallback {

  /**
   * Return MetaData to be used on remove due to eviction. May return null.
   */
  MetaData getEvictRemoveMetaData();
}
