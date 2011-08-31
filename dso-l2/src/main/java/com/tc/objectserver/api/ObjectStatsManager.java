/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;

public interface ObjectStatsManager {
  public ObjectStatsManager NULL_OBJECT_STATS_MANAGER = new ObjectStatsManager() {
                                                        public String getObjectTypeFromID(ObjectID id) {
                                                          return "";
                                                        }
                                                      };

  /**
   * This method will be used by lock stats manager when lock statistics is enabled and will return the type of the
   * object
   * 
   * @param id - identifier of the object
   * @param cacheOnly - return type if present only in cache.
   */
  public String getObjectTypeFromID(ObjectID id);
}
