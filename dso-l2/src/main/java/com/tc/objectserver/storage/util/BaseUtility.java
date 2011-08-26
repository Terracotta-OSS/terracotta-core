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
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.persistence.db.SerializationAdapterFactory;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

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

  private DBPersistorImpl createPersistor(int id) throws Exception {
    DBFactory factory = getDBFactory();
    DBEnvironment env = factory.createEnvironment(true, databaseDirs[id - 1]);
    dbEnvironmentsMap.put(id, env);
    SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
    final TestManagedObjectChangeListenerProvider managedObjectChangeListenerProvider = new TestManagedObjectChangeListenerProvider();
    DBPersistorImpl persistor = new DBPersistorImpl(logger, env, serializationAdapterFactory);
    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);
    return persistor;
  }

  private DBFactory getDBFactory() {
    String factoryName = TCPropertiesImpl.getProperties().getProperty(TCPropertiesConsts.L2_DB_FACTORY_NAME);
    DBFactory dbFactory = null;
    try {
      Class dbClass = Class.forName(factoryName);
      Constructor<DBFactory> constructor = dbClass.getConstructor(TCProperties.class);
      dbFactory = constructor.newInstance(TCPropertiesImpl.getProperties().getPropertiesFor("l2"));
    } catch (Exception e) {
      logger.error("Failed to create db factory of class: " + factoryName, e);
    }
    return dbFactory;
  }

  protected DBPersistorImpl getPersistor(int id) {
    return (DBPersistorImpl) dbPersistorsMap.get(Integer.valueOf(id));
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
