/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import java.io.File;

public class SleepycatDBUsageTest extends AbstractDBUtilsTestBase {

  public void testSleepycatDBUsageTest() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data");
    databaseDir.mkdirs();

    SleepycatPersistor sleepycatPersistor = getSleepycatPersistor(databaseDir);
    populateSleepycatDB(sleepycatPersistor);
    sleepycatPersistor.close();

    SleepycatDBUsage sleepycatDBUsage = new SleepycatDBUsage(databaseDir);
    sleepycatDBUsage.report();
    assertEquals(123, sleepycatDBUsage.totalCount);
    assertEquals(1012, sleepycatDBUsage.keyTotal);
    assertEquals(16287, sleepycatDBUsage.valuesTotal);
    assertEquals(17299, sleepycatDBUsage.grandTotal);
  }

}
