/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import org.apache.commons.io.FileUtils;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.db.DBSequenceKeys;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBPersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBSequence;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBSequenceTest extends TCTestCase {
  private File                  envHome;
  private EnvironmentConfig     ecfg;
  private DatabaseConfig        dbcfg;
  private BerkeleyDBEnvironment env;
  private static int            count = 0;

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
    env = newEnv(true);

  }

  public void tearDown() throws Exception {
    super.tearDown();
    env.close();
    envHome.delete();
    FileUtils.cleanDirectory(envHome);
  }

  private BerkeleyDBEnvironment newEnv(boolean paranoid) throws IOException {
    return newEnv(new HashMap(), new ArrayList(), paranoid);
  }

  private BerkeleyDBEnvironment newEnv(Map map, List list, boolean paranoid) throws IOException {
    return new BerkeleyDBEnvironment(map, list, paranoid, envHome, ecfg, dbcfg);
  }

  public void testUID() throws Exception {
    assertTrue(env.open());
    BerkeleyDBPersistenceTransactionProvider persistenceTransactionProvider = new BerkeleyDBPersistenceTransactionProvider(
                                                                                                                         env
                                                                                                                             .getEnvironment());
    TCLogger logger = TCLogging.getLogger(DBSequenceTest.class);
    BerkeleyDBSequence sequence = (BerkeleyDBSequence) env.getSequence(persistenceTransactionProvider, logger,
                                                                     DBSequenceKeys.CLIENTID_SEQUENCE_NAME, 1);
    String uid1 = sequence.getUID();
    assertNotNull(uid1);
    System.err.println("UID is " + uid1);
    sequence = (BerkeleyDBSequence) env.getSequence(persistenceTransactionProvider, logger,
                                                   DBSequenceKeys.CLIENTID_SEQUENCE_NAME, 1);
    String uid2 = sequence.getUID();
    System.err.println("UID is " + uid2);
    assertEquals(uid1, uid2);
    sequence = (BerkeleyDBSequence) env.getSequence(persistenceTransactionProvider, logger,
                                                   DBSequenceKeys.TRANSACTION_SEQUENCE_DB_NAME, 1);
    String uid3 = sequence.getUID();
    System.err.println("UID is " + uid3);
    assertNotEquals(uid1, uid3);
  }

  public void testBasic() throws Exception {
    assertTrue(env.open());
    BerkeleyDBPersistenceTransactionProvider persistenceTransactionProvider = new BerkeleyDBPersistenceTransactionProvider(
                                                                                                                         env
                                                                                                                             .getEnvironment());
    TCLogger logger = TCLogging.getLogger(DBSequenceTest.class);
    BerkeleyDBSequence sequence = (BerkeleyDBSequence) env.getSequence(persistenceTransactionProvider, logger,
                                                                     DBSequenceKeys.CLIENTID_SEQUENCE_NAME, 1);
    long id = sequence.next();
    assertEquals(1, id);
    id = sequence.nextBatch(100);
    assertEquals(2, id);
    id = sequence.next();
    assertEquals(102, id);
    id = sequence.next();
    assertEquals(103, id);
    id = sequence.next();
    assertEquals(104, id);
    sequence.setNext(1000);
    id = sequence.next();
    assertEquals(1000, id);
    id = sequence.nextBatch(100);
    assertEquals(1001, id);
    id = sequence.nextBatch(100);
    assertEquals(1101, id);
    boolean failed = false;
    try {
      sequence.setNext(100);
      failed = true;
    } catch (AssertionError er) {
      // expected
    }
    id = sequence.next();
    assertEquals(1201, id);
    id = sequence.current();
    assertEquals(1202, id);
    if (failed) { throw new AssertionError("Didn't fail"); }

    closeDBAndCheckSequence();
  }

  public void closeDBAndCheckSequence() throws Exception {
    Environment environment = new Environment(envHome, ecfg);
    Database db = environment.openDatabase(null, "TEST_FOR_SEQ", dbcfg);
    String KEY_NAME = "SEQUENCE_TEST";

    BerkeleyDBPersistenceTransactionProvider persistenceTransactionProvider = new BerkeleyDBPersistenceTransactionProvider(
                                                                                                                         env
                                                                                                                             .getEnvironment());
    TCLogger logger = TCLogging.getLogger(DBSequenceTest.class);
    BerkeleyDBSequence sequence = new BerkeleyDBSequence(persistenceTransactionProvider, logger, KEY_NAME, 1, db);

    for (int i = 0; i < 10; i++) {
      sequence.nextBatch(2);
    }

    db.close();
    db = environment.openDatabase(null, "TEST_FOR_SEQ", dbcfg);
    sequence = new BerkeleyDBSequence(persistenceTransactionProvider, logger, KEY_NAME, 1, db);

    long seqnum = sequence.nextBatch(2);
    Assert.assertEquals(21, seqnum);

    db.close();
    environment.close();
  }

  public void testLongBatchSize() throws Exception {
    assertTrue(env.open());
    BerkeleyDBPersistenceTransactionProvider persistenceTransactionProvider = new BerkeleyDBPersistenceTransactionProvider(
                                                                                                                         env
                                                                                                                             .getEnvironment());
    TCLogger logger = TCLogging.getLogger(DBSequenceTest.class);
    BerkeleyDBSequence sequence = (BerkeleyDBSequence) env.getSequence(persistenceTransactionProvider, logger,
                                                                     DBSequenceKeys.CLIENTID_SEQUENCE_NAME, 1);
    long id = sequence.next();
    assertEquals(1, id);
    long batchSize = Integer.MAX_VALUE * 2L;
    System.out.println("Testing with batch size = " + batchSize);
    id = sequence.nextBatch(batchSize);
    assertEquals(2, id);
    id = sequence.next();
    assertEquals(batchSize + 2, id);

    closeDBAndCheckSequence();
  }

}
