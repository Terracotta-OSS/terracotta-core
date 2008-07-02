/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import java.io.File;

public class SleepycatDBUsageTest extends AbstractDBUtilsTestBase {

  public void testSleepycatDBUsageTest() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data-test1");
    databaseDir.mkdirs();

    SleepycatPersistor sleepycatPersistor = getSleepycatPersistor(databaseDir);
    populateSleepycatDB(sleepycatPersistor);
    sleepycatPersistor.close();

    SleepycatDBUsage sleepycatDBUsage_test1 = new SleepycatDBUsage(databaseDir);
    sleepycatDBUsage_test1.report();

    assertTrue(sleepycatDBUsage_test1.getTotalCount() > 0);
    assertTrue(sleepycatDBUsage_test1.getKeyTotal() > 0);
    assertTrue(sleepycatDBUsage_test1.getValuesTotal() > 0);
    assertTrue(sleepycatDBUsage_test1.getGrandTotal() > 0);

    databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data-test2");
    databaseDir.mkdirs();
    sleepycatPersistor = getSleepycatPersistor(databaseDir);
    sleepycatPersistor.close();

    // db is not populated
    SleepycatDBUsage sleepycatDBUsage_test2 = new SleepycatDBUsage(databaseDir);
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
