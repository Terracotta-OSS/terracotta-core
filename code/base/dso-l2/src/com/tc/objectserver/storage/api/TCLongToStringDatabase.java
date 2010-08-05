/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import gnu.trove.TLongObjectHashMap;

public interface TCLongToStringDatabase {
  /**
   * returns the Map with <long, String> key value pairs.
   */
  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target, PersistenceTransaction tx);

  /**
   * puts a <long, String> key value pair to the db.
   */
  public Status put(long val, String string, PersistenceTransaction tx);
}
