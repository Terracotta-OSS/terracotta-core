/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;


import com.tc.objectserver.persistence.db.AbstractDBUtilsTestBase;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.storage.util.FastLoadOidlogAnalysis;
import com.tc.objectserver.storage.util.FastLoadOidlogAnalysis.OidlogsStats;

import java.io.File;
import java.util.List;

public class FastLoadOidLogAnalysisTest extends AbstractDBUtilsTestBase {
  
  public void testFastLoadOidLogAnalysis() throws Exception {

    File databaseDir = new File(getTempDirectory().toString() + File.separator + "db-data");
    databaseDir.mkdirs();

    DBPersistorImpl sleepycatPersistor = getSleepycatPersistor(databaseDir);
    populateSleepycatDB(sleepycatPersistor);
    sleepycatPersistor.close();
    
    FastLoadOidlogAnalysis fastLoadOidlogAnalysis = new FastLoadOidlogAnalysis(databaseDir);
    fastLoadOidlogAnalysis.report();
    List list = fastLoadOidlogAnalysis.oidlogsStatsList;
    assertEquals(list.size(), 1);
    OidlogsStats stats = (OidlogsStats)list.get(0);
    assertEquals("oid_store_log", stats.getDatabaseName());
    assertEquals(2, stats.getAddCount());
    assertEquals(0, stats.getDeleteCount());
    assertEquals(1000, stats.getStartSeqence());
    assertEquals(1001, stats.getEndSequence());
  
  }

}
