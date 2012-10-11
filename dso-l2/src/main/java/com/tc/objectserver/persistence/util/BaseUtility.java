/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.util;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProvider;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListener;
import com.tc.objectserver.persistence.gb.GBPersistor;
import com.tc.objectserver.persistence.gb.StorageManagerFactory;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.heap.HeapStorageManager;

public abstract class BaseUtility {

  private static final TCLogger logger = TCLogging.getLogger(BaseUtility.class);

  protected final Writer        writer;
  protected final Map           dbPersistorsMap;
  protected final Map           dbEnvironmentsMap;
  protected final File[]        databaseDirs;

  public BaseUtility(Writer writer, File[] databaseDirs) throws Exception {
    this.writer = writer;
    this.databaseDirs = databaseDirs;
    dbPersistorsMap = new HashMap(databaseDirs.length);
    dbEnvironmentsMap = new HashMap(databaseDirs.length);
    initPersistors(databaseDirs.length);
  }

  private void initPersistors(int persistorCount) throws Exception {
    ManagedObjectStateFactory.disableSingleton(true);
    for (int i = 1; i <= persistorCount; i++) {
      dbPersistorsMap.put(Integer.valueOf(i), createPersistor(i));
    }
  }

  private GBPersistor createPersistor(int id) throws Exception {
    final TestManagedObjectChangeListenerProvider managedObjectChangeListenerProvider = new TestManagedObjectChangeListenerProvider();
    GBPersistor persistor = new GBPersistor(new StorageManagerFactory() {

          @Override
          public StorageManager createStorageManager(Map<String, KeyValueStorageConfig<?, ?>> configMap) {
              return new HeapStorageManager();
          }
      });
    
    return persistor;
  }

  protected GBPersistor getPersistor(int id) {
    return (GBPersistor) dbPersistorsMap.get(Integer.valueOf(id));
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