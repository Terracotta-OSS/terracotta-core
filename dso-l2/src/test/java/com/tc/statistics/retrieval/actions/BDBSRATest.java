/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;

public class BDBSRATest extends TCTestCase {

  public void test() throws Exception {
    File dbHome = newDBHome();
    BerkeleyDBEnvironment env = new BerkeleyDBEnvironment(true, dbHome);
    env.open();

    SRAForBerkeleyDB sras = new SRAForBerkeleyDB(env);
    sras.retrieveStatisticData();
    ThreadUtil.reallySleep(10 * 1000);
    // check for SRAs to be 0
    StatisticRetrievalAction[] sraList = sras.getBDBSRAs();
    checkCacheStats(sraList);
    checkIOStats(sraList);
    checkCleanerStats(sraList);
    checkLoggingStats(sraList);
  }

  private void checkLoggingStats(StatisticRetrievalAction[] sraList) {
    SRABDBLogging sraLogging = null;
    for (int i = 0; i < sraList.length; i++) {
      if (sraList[i] instanceof SRABDBLogging) {
        sraLogging = (SRABDBLogging) sraList[i];
      }
    }
    assertNotNull(sraLogging);

    assertNotAll0(sraLogging.retrieveStatisticData());
  }

  private void checkCleanerStats(StatisticRetrievalAction[] sraList) {
    SRABDBCleaner sraCleaner = null;
    for (int i = 0; i < sraList.length; i++) {
      if (sraList[i] instanceof SRABDBCleaner) {
        sraCleaner = (SRABDBCleaner) sraList[i];
      }
    }
    assertNotNull(sraCleaner);
  }

  private void checkIOStats(StatisticRetrievalAction[] sraList) {
    SRABDBIO sraIO = null;
    for (int i = 0; i < sraList.length; i++) {
      if (sraList[i] instanceof SRABDBIO) {
        sraIO = (SRABDBIO) sraList[i];
      }
    }
    assertNotNull(sraIO);
  }

  private void checkCacheStats(StatisticRetrievalAction[] sraList) {
    SRABDBCache sraCache = null;
    for (int i = 0; i < sraList.length; i++) {
      if (sraList[i] instanceof SRABDBCache) {
        sraCache = (SRABDBCache) sraList[i];
      }
    }
    assertNotNull(sraCache);

    assertNotAll0(sraCache.retrieveStatisticData());
  }

  public void assertNotAll0(StatisticData[] datas) {
    boolean result = false;
    for (int i = 0; i < datas.length; i++) {
      if (0L != ((Long) datas[i].getData()).longValue()) {
        result = true;
        break;
      }
    }
    Assert.assertTrue(result);
  }

  private File newDBHome() throws IOException {
    File file = new File(getTempDirectory(), "db");
    assertFalse(file.exists());
    return file;
  }
}
