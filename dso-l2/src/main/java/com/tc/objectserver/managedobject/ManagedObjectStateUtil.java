/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ManagedObject.ManagedObjectCacheStrategy;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class ManagedObjectStateUtil {
  private static final boolean PIN_SEGMENTS = TCPropertiesImpl
                                                .getProperties()
                                                .getBoolean(
                                                            TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PINSEGMENTS_ENABLED);

  public static ManagedObjectCacheStrategy getCacheStrategy(final ManagedObjectState state) {
    final byte type = state.getType();
    if (PIN_SEGMENTS && type == ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE) { return ManagedObjectCacheStrategy.PINNED; }

    return ManagedObjectCacheStrategy.UNPINNED;
  }

}
