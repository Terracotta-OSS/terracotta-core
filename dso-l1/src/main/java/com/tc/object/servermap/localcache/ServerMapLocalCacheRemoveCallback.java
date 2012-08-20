/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.TCObjectSelf;

public interface ServerMapLocalCacheRemoveCallback {

  public void removedElement(AbstractLocalCacheStoreValue value);

  public void removedElement(TCObjectSelf removed);
}
