/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.objectserver.api.GCStats;
import com.tc.stats.DSOMBean;
import com.tc.util.Assert;
import com.tctest.runner.PostAction;

import java.util.List;

public abstract class GCAndActivePassiveTestBase extends ActivePassiveTransparentTestBase implements TestConfigurator {

  protected GCConfigurationHelper gcConfigHelper = new GCConfigurationHelper();
  
  public void setUp() throws Exception {
    super.setUp();
    doSetUp(this);
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(gcConfigHelper.getNodeCount())
        .setIntensity(GCConfigurationHelper.Parameters.LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    gcConfigHelper.setupConfig(configFactory);
  }

  protected static class VerifyDGCPostAction implements PostAction {

    protected List<DSOMBean> dsoMBeans;

    public VerifyDGCPostAction(List<DSOMBean> dsoMBeans) {
      this.dsoMBeans = dsoMBeans;
    }

    public void execute() {
      int totalObjects = 0;
      synchronized (this.dsoMBeans) {
        for (int server = 0; server < this.dsoMBeans.size(); ++server) {
          DSOMBean mbean = this.dsoMBeans.get(server);
          int liveObjects = mbean.getLiveObjectCount();
          GCStats[] stats = mbean.getGarbageCollectorStats();
          System.out.println("XXX Server[" + server + "] live objects = " + liveObjects);
          int gcObjects = 0;
          for (int i = 0; i < stats.length; ++i) {
            System.out.println("XXX Server[" + server + "] stats[" + i + "] garbage "
                               + stats[i].getActualGarbageCount());
            gcObjects += Long.valueOf(stats[i].getActualGarbageCount());
          }
          System.out.println("XXX Server[" + server + "] total objects = " + (liveObjects + gcObjects));
          if (server == 0) {
            totalObjects = liveObjects + gcObjects;
          } else {
            Assert.assertEquals(totalObjects, liveObjects + gcObjects);
          }
        }
      }
    }
  }
}
