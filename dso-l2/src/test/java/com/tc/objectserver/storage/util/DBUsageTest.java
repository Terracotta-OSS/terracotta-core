/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import com.tc.objectserver.persistence.db.AbstractDBUtilsTestBase;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBStatisticsHandler;

import java.io.File;
import java.io.OutputStreamWriter;

public class DBUsageTest extends AbstractDBUtilsTestBase {

  public void testSleepycatDBUsageTest() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data-test1");
    databaseDir.mkdirs();

    DBPersistorImpl sleepycatPersistor = getSleepycatPersistor(databaseDir);
    populateSleepycatDB(sleepycatPersistor);
    sleepycatPersistor.close();

    dbenv.open();
    BerkeleyDBStatisticsHandler sleepycatDBUsage_test1 = new BerkeleyDBStatisticsHandler(
                                                                                         dbenv.getEnvironment(),
                                                                                         new OutputStreamWriter(
                                                                                                                System.out));
    sleepycatDBUsage_test1.report();

    assertTrue(sleepycatDBUsage_test1.getTotalCount() > 0);
    assertTrue(sleepycatDBUsage_test1.getKeyTotal() > 0);
    assertTrue(sleepycatDBUsage_test1.getValuesTotal() > 0);
    assertTrue(sleepycatDBUsage_test1.getGrandTotal() > 0);

    databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data-test2");
    databaseDir.mkdirs();
    sleepycatPersistor = getSleepycatPersistor(databaseDir);
    // By getObjectCount() to wait for completion of ObjectIdReaderThread
    sleepycatPersistor.getManagedObjectPersistor().getObjectCount();
    // By snapshot to wait for completion of ObjectIdReaderThread
    sleepycatPersistor.getManagedObjectPersistor().snapshotMapTypeObjectIDs();
    sleepycatPersistor.getManagedObjectPersistor().snapshotEvictableObjectIDs();
    sleepycatPersistor.close();

    // db is not populated
    dbenv.open();
    BerkeleyDBStatisticsHandler sleepycatDBUsage_test2 = new BerkeleyDBStatisticsHandler(
                                                                                         dbenv.getEnvironment(),
                                                                                         new OutputStreamWriter(
                                                                                                                System.out));
    sleepycatDBUsage_test2.report();

    assertTrue(sleepycatDBUsage_test2.getTotalCount() > 0);
    assertTrue(sleepycatDBUsage_test2.getKeyTotal() > 0);
    assertTrue(sleepycatDBUsage_test2.getValuesTotal() > 0);
    assertTrue(sleepycatDBUsage_test2.getGrandTotal() > 0);

    assertTrue(sleepycatDBUsage_test1.getGrandTotal() >= sleepycatDBUsage_test2.getGrandTotal());
    assertTrue(sleepycatDBUsage_test1.getKeyTotal() >= sleepycatDBUsage_test2.getKeyTotal());
    assertTrue(sleepycatDBUsage_test1.getTotalCount() >= sleepycatDBUsage_test2.getTotalCount());
    assertTrue(sleepycatDBUsage_test1.getValuesTotal() >= sleepycatDBUsage_test2.getValuesTotal());

  }

}
