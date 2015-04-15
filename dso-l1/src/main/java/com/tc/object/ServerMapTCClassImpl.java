/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
