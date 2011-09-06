/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

public interface ServerMapLocalCacheRemoveCallback {

  public void removedElement(Object key, AbstractLocalCacheStoreValue value);
}
