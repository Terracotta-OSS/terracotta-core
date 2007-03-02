package com.tctest.modules.lucene_2_0_0;

import org.terracotta.modules.lucene_2_0_0.SimpleLuceneDistributedIndexApp;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class SimpleLuceneDistributedIndexTest extends TransparentTestBase {

  private static final int TIMEOUT = 10 * 60 * 1000; // 10min;

  protected Class getApplicationClass() {
    return SimpleLuceneDistributedIndexApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(2);
    t.getTransparentAppConfig().setIntensity(1);
    t.initializeTestRunner();
    t.getRunnerConfig().executionTimeout = TIMEOUT;
  }
}
