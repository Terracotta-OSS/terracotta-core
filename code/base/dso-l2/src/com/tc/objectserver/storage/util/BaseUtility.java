/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProvider;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListener;
import com.tc.objectserver.persistence.db.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.db.SerializationAdapterFactory;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseUtility {

  private static final TCLogger logger                = TCLogging.getLogger(BaseUtility.class);

  protected final Writer writer;
  protected final Map    sleepycatPersistorMap;
  protected final File   [] databaseDirs;
  public BaseUtility(Writer writer, File[] databaseDirs)throws Exception {
    this.writer = writer;
    this.databaseDirs = databaseDirs;
    sleepycatPersistorMap = new HashMap(databaseDirs.length);
    initPersistors(databaseDirs.length);
  }

  private void initPersistors(int persistorCount) throws Exception {
    ManagedObjectStateFactory.disableSingleton(true);
    for (int i = 1; i <= persistorCount; i++) {
      sleepycatPersistorMap.put(new Integer(i), createPersistor(i));
    }
  }

  private DBPersistorImpl createPersistor(int id) throws Exception {
    BerkeleyDBEnvironment env = new BerkeleyDBEnvironment(true, databaseDirs[id -1]);
    SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
    final TestManagedObjectChangeListenerProvider managedObjectChangeListenerProvider = new TestManagedObjectChangeListenerProvider();
    DBPersistorImpl persistor = new DBPersistorImpl(logger, env, serializationAdapterFactory);
    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor); 
    return persistor;
  }
  
  protected DBPersistorImpl getPersistor(int id) {
    return (DBPersistorImpl)sleepycatPersistorMap.get(new Integer(id));
  }

  protected File getDatabaseDir(int id) {
    return databaseDirs[id - 1];
  }
  
  protected void log(String message) {
    try {
      writer.write(message);
      writer.write("\n");
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private static class TestManagedObjectChangeListenerProvider implements ManagedObjectChangeListenerProvider {

    public ManagedObjectChangeListener getListener() {
      return new NullManagedObjectChangeListener();

    }
  }

}
