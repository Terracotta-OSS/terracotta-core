/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCMapsDatabase;

import java.io.IOException;

public interface PersistableCollection {

  public int commit(TCCollectionsSerializer serializer, PersistenceTransaction tx, TCMapsDatabase db)
  throws IOException, TCDatabaseException;

  public void load(TCCollectionsSerializer serializer, PersistenceTransaction tx, TCMapsDatabase db) throws IOException,
  ClassNotFoundException, TCDatabaseException;

}
