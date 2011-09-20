/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.exception.TCRuntimeException;

public class LocalCacheStoreFullException extends TCRuntimeException {

  public LocalCacheStoreFullException(Throwable t) {
    super(t);
  }

}
