/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;
import com.tc.object.ObjectID;
import com.tc.util.OidLongArray;

public interface OidBitsArrayMap {
  
  public boolean contains(ObjectID id);

  public OidLongArray getAndSet(ObjectID id);

  public OidLongArray getAndClr(ObjectID id);

  /*
   * clear in-memory data
   */
  public void clear();

  /*
   * get base of OidLongArray contains ObjectID
   */
  public Long oidIndex(ObjectID id);
  
  /*
   * flush in-memory entry to disk
   */
  public void updateToDisk(Transaction tx) throws DatabaseException;
  
}
