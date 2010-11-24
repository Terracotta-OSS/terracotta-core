/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.impl.StatisticsGathererImpl;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.store.StatisticDataUser;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;
import com.tc.util.UUID;
import com.tctest.TransparentTestIface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StatisticsGathererConfigSampleRateTest extends AbstractStatisticsTransparentTestBase {
  @Override
  protected void duringRunningCluster() throws Exception {
    waitForAllNodesToConnectToGateway(StatisticsGathererTestApp.NODE_COUNT+1);

    File tmp_dir = makeTmpDir(getClass());

    StatisticsStore store = new H2StatisticsStoreImpl(tmp_dir);
    StatisticsGatherer gatherer = new StatisticsGathererImpl(store);

    gatherer.connect("localhost", getAdminPort());

    String sessionid = UUID.getUUID().toString();
    gatherer.createSession(sessionid);
    gatherer.enableStatistics(gatherer.getSupportedStatistics());
    gatherer.startCapturing();
    Thread.sleep(10000);
    gatherer.stopCapturing();
    Thread.sleep(5000);
    gatherer.closeSession();

    final List<StatisticData> data_list1 = new ArrayList<StatisticData>();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        data_list1.add(data);
        return true;
      }
    });

    gatherer.setGlobalParam(DSOStatisticsConfig.KEY_RETRIEVER_SCHEDULE_INTERVAL, StatisticsRetriever.DEFAULT_GLOBAL_FREQUENCY / 2);

    sessionid = UUID.getUUID().toString();
    gatherer.createSession(sessionid);
    gatherer.enableStatistics(gatherer.getSupportedStatistics());
    gatherer.startCapturing();
    Thread.sleep(10000);
    gatherer.stopCapturing();
    Thread.sleep(5000);
    gatherer.closeSession();

    final List<StatisticData> data_list2 = new ArrayList<StatisticData>();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        data_list2.add(data);
        return true;
      }
    });

    assertTrue(data_list1.size() * 2 <= data_list2.size());

    gatherer.disconnect();
  }

  @Override
  protected Class getApplicationClass() {
    return StatisticsGathererTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsGathererTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}