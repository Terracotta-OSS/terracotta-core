/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.EnvironmentConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.db.TCMapStore;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBPersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.DBSequenceTest;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBMapStoreTest extends TCTestCase {

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

  public void testBasic() throws Exception {
    BerkeleyDBEnvironment env = newEnv(true);
    assertTrue(env.open());
    BerkeleyDBPersistenceTransactionProvider persistenceTransactionProvider = new BerkeleyDBPersistenceTransactionProvider(
                                                                                                                         env
                                                                                                                             .getEnvironment());
    TCLogger logger = TCLogging.getLogger(DBSequenceTest.class);

    TCMapStore mapStore = new TCMapStore(persistenceTransactionProvider, logger, env
        .getClusterStateStoreDatabase());
    String val = mapStore.get("Hello");
    assertNull(val);
    mapStore.put("Hello", "Saro");
    val = mapStore.get("Hello");
    assertEquals("Saro", val);
    boolean removed = mapStore.remove("Hello");
    assertTrue(removed);
    val = mapStore.get("Hello");
    assertNull(val);

    for (int i = 0; i < 1000; i++) {
      mapStore.put("day" + i, "cya" + i);
    }

    removed = mapStore.remove("Day1"); // not found
    assertFalse(removed);

    for (int i = 0; i < 1000; i++) {
      val = mapStore.get("day" + i);
      assertEquals("cya" + i, val);
      removed = mapStore.remove("day" + i);
      assertTrue(removed);
    }

    for (int i = 0; i < 1000; i++) {
      val = mapStore.get("day" + i);
      assertNull(val);
      removed = mapStore.remove("day" + i);
      assertFalse(removed);
    }
  }

}
