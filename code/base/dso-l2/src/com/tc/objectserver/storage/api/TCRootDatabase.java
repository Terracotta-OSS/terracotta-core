/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TCRootDatabase {
  /**
   * Puts the root name with the id to the db
   */
  public Status insert(byte[] rootName, long id, PersistenceTransaction tx);

  /**
   * gets the id from the root name
   */
  public long getIdFromName(byte[] rootName, PersistenceTransaction tx);

  /**
   * Returns a map from root names to the id
   */
  public Map<byte[], Long> getRootNamesToId(PersistenceTransaction tx);

  /**
   * Returns the root names in bytes. It is the responsibility of this method to commit the transaction.
   */
  public List<byte[]> getRootNames(PersistenceTransaction tx);

  /**
   * Returns the Root Object Ids. It is the responsibility of this method to commit the transaction.
   */
  public Set<ObjectID> getRootIds(PersistenceTransaction tx);
}
