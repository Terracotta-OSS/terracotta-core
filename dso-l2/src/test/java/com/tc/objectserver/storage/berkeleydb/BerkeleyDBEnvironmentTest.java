/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import org.apache.commons.lang.ArrayUtils;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.objectserver.persistence.db.DatabaseNotOpenException;
import com.tc.objectserver.persistence.db.DatabaseOpenException;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBTCLongToBytesDatabase;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment.ClassCatalogWrapper;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * IMPORTANT: Sleepycat uses a static cache. If you open an environment, but don't close it, even if you delete the data
 * files underneath it, when you create another instance of the environment on the same directory, the data may still be
 * there in cache. This makes it difficult to test in a JUnit scenario.
 */
public class BerkeleyDBEnvironmentTest extends TCTestCase {
  private File              envHome;
  private EnvironmentConfig ecfg;
  private DatabaseConfig    dbcfg;
  private static int        count = 0;

  protected void setUp() throws Exception {
    super.setUp();
    ecfg = new EnvironmentConfig();
    ecfg.setAllowCreate(true);
    ecfg.setReadOnly(false);
    ecfg.setTransactional(true);

    dbcfg = new DatabaseConfig();
    dbcfg.setAllowCreate(true);
    dbcfg.setReadOnly(false);
    dbcfg.setTransactional(true);
    while ((envHome = new File(this.getTempDirectory(), ++count + "")).exists()) {
      //
    }
    System.out.println("DB home: " + envHome);

  }

  private BerkeleyDBEnvironment newEnv(boolean paranoid) throws IOException {
    return newEnv(new HashMap(), new ArrayList(), paranoid);
  }

  private BerkeleyDBEnvironment newEnv(Map map, List list, boolean paranoid) throws IOException {
    return new BerkeleyDBEnvironment(map, list, paranoid, envHome, ecfg, dbcfg);
  }

  public void testCrashParanoidReopenParanoid() throws Exception {
    BerkeleyDBEnvironment env = newEnv(true);
    assertTrue(env.open());
    env.forceClose();
    env = newEnv(true);
    // this should succeed, since a paranoid database should always be clean.
    assertTrue(env.open());
    env.close();
  }

  public void testCrashNotParanoidReopenNotParanoid() throws Exception {
    BerkeleyDBEnvironment env = newEnv(false);
    assertCleanDir();
    assertTrue(env.open());
    env.forceClose();
    env = newEnv(false);
    // this shouldn't open clean, since the database wasn't opened in paranoid
    // mode before it crashed
    assertFalse(env.open());
    env.forceClose();
  }

  public void testCrashParanoidReopenNotParanoid() throws Exception {
    BerkeleyDBEnvironment env = newEnv(true);
    assertTrue(env.open());
    env.forceClose();
    env = newEnv(false);
    // this should succeed, since a paranoid database should always be clean
    assertTrue(env.open());
    env.forceClose();
  }

  private void assertCleanDir() throws Exception {
    assertEquals(Arrays.asList(new Object[] {}), Arrays.asList(this.envHome.listFiles()));
  }

  public void testCrashNotParanoidReopenParanoid() throws Exception {
    BerkeleyDBEnvironment env = newEnv(false);
    assertTrue(env.open());
    env.forceClose();
    env = newEnv(true);
    // this shouldn't open clean, since the database wasn't opened in paranoid
    // mode
    // before it crashed.
    assertFalse(env.open());
  }

  public void testLifecycleParanoid() throws Exception {
    testLifecycle(true);
  }

  public void testLifecycleNotParanoid() throws Exception {
    testLifecycle(false);
  }

  private void testLifecycle(boolean paranoid) throws Exception {
    List databases = new LinkedList();
    Map databasesByName = new HashMap();
    assertFalse(this.envHome.exists());
    BerkeleyDBEnvironment env = newEnv(databasesByName, databases, paranoid);

    try {
      env.getEnvironment();
      fail("Should have thrown an exception trying to get the environment before open()");
    } catch (DatabaseNotOpenException e) {
      // ok.
    }

    try {
      env.getObjectDatabase();
      fail("Should have thrown an exception trying to get the database before open()");
    } catch (DatabaseNotOpenException e) {
      // ok.
    }

    try {
      env.close();
      fail("Should have thrown an exception trying to close the database before open()");
    } catch (DatabaseNotOpenException e) {
      // ok.
    }

    try {
      env.getClassCatalogWrapper();
      fail("Should have thrown an exception trying to get the class catalog before open()");
    } catch (DatabaseNotOpenException e) {
      // ok.
    }
    assertEquals(0, databases.size());
    assertEquals(databasesByName.size(), databases.size());
    boolean result = env.open();

    // the first time the database is opened, it should be brand new and clean.
    assertTrue(result);

    assertEquals(databasesByName.size(), databases.size());
    assertDatabasesOpen(databases);

    try {
      env.open();
      fail("Should have thrown an exception trying to open the environment twice.");
    } catch (DatabaseOpenException e) {
      // ok.
    }

    ClassCatalogWrapper cc = env.getClassCatalogWrapper();
    env.close();
    assertDatabasesClosed(databases);
    assertClassCatalogClosed(cc);

    try {
      env.close();
      fail("Should have thrown an exception trying to open the environment after close()");
    } catch (DatabaseNotOpenException e) {
      // ok.
    }

    databases.clear();
    databasesByName.clear();

    env = newEnv(databasesByName, databases, paranoid);
    result = env.open();
    assertTrue(result);

    Database db = ((BerkeleyDBTCLongToBytesDatabase) env.getObjectDatabase()).getDatabase();

    DatabaseEntry key = new DatabaseEntry(new byte[] { 1 });
    DatabaseEntry one = new DatabaseEntry(new byte[] { 1 });
    DatabaseEntry two = new DatabaseEntry(new byte[] { 2 });

    DatabaseEntry value = new DatabaseEntry();

    OperationStatus status = db.get(null, key, value, LockMode.DEFAULT);
    assertEquals(OperationStatus.NOTFOUND, status);

    status = db.put(null, key, one);
    assertEquals(OperationStatus.SUCCESS, status);

    status = db.get(null, key, value, LockMode.DEFAULT);
    assertEquals(OperationStatus.SUCCESS, status);
    assertTrue(ArrayUtils.isEquals(one.getData(), value.getData()));

    status = db.put(null, key, two);
    assertEquals(OperationStatus.SUCCESS, status);

    status = db.get(null, key, value, LockMode.DEFAULT);
    assertEquals(OperationStatus.SUCCESS, status);
    assertTrue(ArrayUtils.isEquals(two.getData(), value.getData()));

    env.close();

    databases.clear();
    databasesByName.clear();
    env = newEnv(databasesByName, databases, paranoid);
    env.open();
    db = ((BerkeleyDBTCLongToBytesDatabase) env.getObjectDatabase()).getDatabase();
    status = db.get(null, key, value, LockMode.DEFAULT);
    assertTrue(ArrayUtils.isEquals(two.getData(), value.getData()));

    // test closing then opening again.
    env.close();

    databases.clear();
    databasesByName.clear();
    env = newEnv(databasesByName, databases, paranoid);
    env.open();
    db = ((BerkeleyDBTCLongToBytesDatabase) env.getObjectDatabase()).getDatabase();

    status = db.get(null, key, value, LockMode.DEFAULT);
    assertEquals(OperationStatus.SUCCESS, status);
    assertTrue(ArrayUtils.isEquals(two.getData(), value.getData()));

    env.close();
  }

  private void assertClassCatalogClosed(ClassCatalogWrapper cc) throws DatabaseException {
    try {
      cc.close();
      fail("Should have thrown an exception.");
    } catch (IllegalStateException e) {
      // ok
    }
  }

  private void assertDatabasesOpen(List databases) throws Exception {
    for (Iterator i = databases.iterator(); i.hasNext();) {
      Object o = i.next();
      Database db = null;
      if (o instanceof AbstractBerkeleyDatabase) {
        db = ((AbstractBerkeleyDatabase) i.next()).getDatabase();
      } else if (o instanceof Database) {
        db = (Database) o;
      }
      assertTrue(isDatabaseOpen(db));
    }
  }

  private void assertDatabasesClosed(List databases) throws Exception {
    for (Iterator i = databases.iterator(); i.hasNext();) {
      Object o = i.next();
      Database db = null;
      if (o instanceof AbstractBerkeleyDatabase) {
        db = ((AbstractBerkeleyDatabase) i.next()).getDatabase();
      } else if (o instanceof Database) {
        db = (Database) o;
      }
      assertFalse(isDatabaseOpen(db));
    }
  }

  private boolean isDatabaseOpen(Database db) throws Exception {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(new byte[] { 1 });
    try {
      db.get(null, key, new DatabaseEntry(), LockMode.DEFAULT);
      return true;
    } catch (IllegalStateException e) {
      // XXX: This may not be a reliable test, but there doesn't seem to be
      // another way to tell.
      return false;
    }

  }
}