/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import com.tc.objectserver.persistence.db.AbstractDBUtilsTestBase;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.storage.util.ManagedObjectReport;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;

public class ManagedObjectReportTest extends AbstractDBUtilsTestBase {

  public void testManagedObjectReport() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data");
    databaseDir.mkdirs();

    ManagedObjectReport managedObjectReport = new ManagedObjectReport(databaseDir);
    DBPersistorImpl sleepycatPersistor = managedObjectReport.getPersistor(1);

    populateSleepycatDB(sleepycatPersistor);
    // wait for checkpoint to flush log to oid store
    ThreadUtil.reallySleep(TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXSLEEP) + 100);
    
    managedObjectReport.report();
    assertEquals(managedObjectReport.totalCounter.get(), 101);
    assertEquals(managedObjectReport.doesNotExistInSet.size(), 101);
    assertEquals(managedObjectReport.objectIDIsNullCounter.get(), 0);
    assertEquals(managedObjectReport.nullObjectIDSet.size(), 0);
    assertEquals(managedObjectReport.classMap.size(), 5);

  }

}
