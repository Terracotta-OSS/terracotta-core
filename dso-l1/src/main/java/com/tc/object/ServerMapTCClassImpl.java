/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.platform.PlatformService;

public class ServerMapTCClassImpl extends TCClassImpl implements TCClass {

  private final L1ServerMapLocalCacheManager globalLocalCacheManager;
  private final RemoteServerMapManager       remoteServerMapManager;
  private final PlatformService              platformService;

  ServerMapTCClassImpl(final PlatformService platformService,
                       final L1ServerMapLocalCacheManager globalLocalCacheManager,
                       final RemoteServerMapManager remoteServerMapManager, final TCClassFactory clazzFactory,
                       final ClientObjectManager objectManager, final Class peer, final boolean useNonDefaultConstructor) {
    super(clazzFactory, objectManager, peer, useNonDefaultConstructor);
    this.platformService = platformService;
    this.globalLocalCacheManager = globalLocalCacheManager;
    this.remoteServerMapManager = remoteServerMapManager;
  }

  @Override
  public TCObject createTCObject(final ObjectID id, final Object pojo, final boolean isNew) {
    if (pojo != null && !(pojo.getClass().getName().equals(TCClassFactory.SERVER_MAP_CLASSNAME))) {
      // bad formatter
      throw new AssertionError("This class should be used only for " + TCClassFactory.SERVER_MAP_CLASSNAME
                               + " but pojo : " + pojo.getClass().getName());
    }
    return new TCObjectServerMapImpl(this.platformService, getObjectManager(), this.remoteServerMapManager, id, pojo,
                                     this, isNew, this.globalLocalCacheManager);
  }

}
