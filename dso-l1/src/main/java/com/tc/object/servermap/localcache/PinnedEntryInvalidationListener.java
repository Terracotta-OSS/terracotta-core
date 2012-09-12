/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

public interface PinnedEntryInvalidationListener {

  public void notifyKeyInvalidated(ServerMapLocalCache cache, Object key, boolean eventual);

}
