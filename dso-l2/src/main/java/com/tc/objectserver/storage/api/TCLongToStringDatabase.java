/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.util.Map;

public interface TCLongToStringDatabase {
  /**
   * returns the Map with <long, String> key value pairs.
   */
  public Map<Long, String> loadMappingsInto(Map<Long, String> target, PersistenceTransaction tx);

  /**
   * puts a <long, String> key value pair to the db.
   */
  public Status insert(long val, String string, PersistenceTransaction tx);
}
