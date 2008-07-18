/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.io.IOException;

public interface PersistableCollection {

  public int commit(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db)
      throws IOException, DatabaseException;

  public void load(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, Database db) throws IOException,
      ClassNotFoundException, DatabaseException;
}
