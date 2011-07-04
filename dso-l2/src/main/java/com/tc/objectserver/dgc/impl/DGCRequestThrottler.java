/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Set;

public class DGCRequestThrottler {
  private static final long     THROTTLE_GC_MILLIS    = TCPropertiesImpl
                                                          .getProperties()
                                                          .getLong(
                                                                   TCPropertiesConsts.L2_OBJECTMANAGER_DGC_THROTTLE_TIME);
  private static final long     REQUESTS_PER_THROTTLE = TCPropertiesImpl
                                                          .getProperties()
                                                          .getLong(
                                                                   TCPropertiesConsts.L2_OBJECTMANAGER_DGC_REQUEST_PER_THROTTLE);
  protected final ObjectManager objectManager;
  private long                  request_count         = 0;

  public DGCRequestThrottler(ObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  public Set<ObjectID> getObjectReferencesFrom(final ObjectID id, final boolean cacheOnly) {
    throttleIfNecessary();
    return objectManager.getObjectReferencesFrom(id, cacheOnly);
  }

  private void throttleIfNecessary() {
    if (THROTTLE_GC_MILLIS > 0 && ++this.request_count % REQUESTS_PER_THROTTLE == 0) {
      ThreadUtil.reallySleep(THROTTLE_GC_MILLIS);
    }
  }
}
