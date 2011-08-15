/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.StageManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.actions.SRAStageQueueDepths;
import com.tc.util.Assert;
import com.tc.util.concurrent.QueueFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import junit.framework.TestCase;

public class SRAStageQueueDepthsTest extends TestCase {

  private StageManager          stageManager;
  private static final String[] STAGE_NAMES = { "DUMMY1", "DUMMY2", "DUMMY3" };

  protected void setUp() throws Exception {
    stageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(StageManagerImpl.class))), new QueueFactory());

    for (int i = 0; i < STAGE_NAMES.length; i++) {
      createStage(STAGE_NAMES[i]);
    }

    stageManager.startAll(new ConfigurationContextImpl(stageManager), Collections.EMPTY_LIST);
  }

  private void createStage(String stageName) {
    stageManager.createStage(stageName, new EventHandler() {
      public void handleEvent(EventContext context) {
        // no-op
      }

      public void handleEvents(Collection context) {
        // no-op
      }

      public void initializeContext(ConfigurationContext context) {
        // no-op
      }

      public void destroy() {
        // no-op
      }
    }, 1, 100);
  }

  public void testRetrieval() {
    SRAStageQueueDepths stageQueueDepths = new SRAStageQueueDepths(stageManager);

    assertStatCollectionStateInManager(false);
    StatisticData[] statisticDatas = stageQueueDepths.retrieveStatisticData();
    Assert.assertEquals(0, statisticDatas.length);

    stageQueueDepths.enableStatisticCollection();
    assertStatCollectionStateInManager(true);
    statisticDatas = stageQueueDepths.retrieveStatisticData();
    Assert.assertEquals(STAGE_NAMES.length, statisticDatas.length);

    for (int i = 0; i < statisticDatas.length; i++) {
      Assert.assertEquals(SRAStageQueueDepths.ACTION_NAME, statisticDatas[i].getName());
      Assert.assertNull(statisticDatas[i].getAgentIp());
      Assert.assertNull(statisticDatas[i].getAgentDifferentiator());
      Assert.assertEquals(STAGE_NAMES[i], statisticDatas[i].getElement());
      Assert.eval("Stage queue depth should be zero", ((Long) statisticDatas[i].getData()).longValue() == 0);
    }

    stageQueueDepths.disableStatisticCollection();
    assertStatCollectionStateInManager(false);
    statisticDatas = stageQueueDepths.retrieveStatisticData();
    Assert.assertEquals(0, statisticDatas.length);

  }

  private void assertStatCollectionStateInManager(final boolean state) {
    for (Iterator stageIterator = stageManager.getStages().iterator(); stageIterator.hasNext();) {
      final Stage stage = (Stage) stageIterator.next();
      if (state) {
        Assert.assertTrue("Stats collection should be true", stage.getSink().isStatsCollectionEnabled());
      } else {
        Assert.assertFalse("Stats collection should be false", stage.getSink().isStatsCollectionEnabled());
      }
    }
  }

  protected void tearDown() throws Exception {
    stageManager = null;
  }
}
