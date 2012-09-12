/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

public interface PinnedEntryFaultCallback {
  public void get(Object key);

  public void unlockedGet(Object key);
}
