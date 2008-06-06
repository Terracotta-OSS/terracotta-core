/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import java.io.File;

public class SleepycatDBUsageTest extends AbstractDBUtilsTest {

  public void testFastLoadOidLogAnalysis() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data");
    databaseDir.mkdirs();

    SleepycatPersistor sleepycatPersistor = getSleepycatPersistor(databaseDir);
    populateSleepycatDB(sleepycatPersistor);
    sleepycatPersistor.close();

    SleepycatDBUsage sleepycatDBUsage = new SleepycatDBUsage(databaseDir);
    sleepycatDBUsage.report();
    assertEquals(122, sleepycatDBUsage.totalCount);
    assertEquals(991, sleepycatDBUsage.valuesTotal);
    assertEquals(16282, sleepycatDBUsage.keyTotal);
    assertEquals(17273, sleepycatDBUsage.grandTotal);

  }

}
