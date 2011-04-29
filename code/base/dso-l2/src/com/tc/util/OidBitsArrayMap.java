/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.api.PersistenceTransaction;

public interface OidBitsArrayMap {

  public boolean contains(ObjectID id);

  public OidLongArray getAndSet(ObjectID id, PersistenceTransaction tx);

  public OidLongArray getAndClr(ObjectID id, PersistenceTransaction tx);

  public void clear();

  public int size();

}
