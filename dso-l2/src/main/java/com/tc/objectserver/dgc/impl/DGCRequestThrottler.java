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
