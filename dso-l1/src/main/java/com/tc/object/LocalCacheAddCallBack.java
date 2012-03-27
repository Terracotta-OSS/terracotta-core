/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public interface LocalCacheAddCallBack {
  /**
   * Discard the local copy of this entry's serialized state.
   */
  void addedToLocalCache();
}
