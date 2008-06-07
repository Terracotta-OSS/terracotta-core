/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import java.io.File;

public class ManagedObjectReportTest extends AbstractDBUtilsTestBase {
 

  public void testManagedObjectReport() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data");
    databaseDir.mkdirs();
  
    ManagedObjectReport managedObjectReport = new ManagedObjectReport(databaseDir);
    SleepycatPersistor sleepycatPersistor = managedObjectReport.getSleepycatPersistor();
    
    populateSleepycatDB(sleepycatPersistor);

    managedObjectReport.report();
    assertEquals(managedObjectReport.totalCounter.get(), 202);
    assertEquals(managedObjectReport.doesNotExistInSet.size(), 101);
    assertEquals(managedObjectReport.objectIDIsNullCounter.get(), 0);
    assertEquals(managedObjectReport.nullObjectIDSet.size(), 0);
    assertEquals(managedObjectReport.classMap.size(), 5);

  }

 

}
