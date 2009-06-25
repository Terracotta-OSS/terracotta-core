/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.StatisticsGathererListener;
import com.tc.statistics.gatherer.impl.StatisticsGathererImpl;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.statistics.store.StatisticDataUser;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;
import com.tc.util.UUID;
import com.tctest.TransparentTestIface;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatisticsGathererTest extends AbstractStatisticsTransparentTestBase implements StatisticsGathererListener {
  private volatile String listenerConnected = null;
  private volatile boolean listenerDisconnected = false;
  private volatile boolean listenerInitialized = false;
  private volatile String listenerCapturingStarted = null;
  private volatile String listenerCapturingStopped = null;
  private volatile String listenerSessionCreated = null;
  private volatile String listenerSessionClosed = null;
  private volatile String[] listenerStatisticsEnabled = null;

  public void connected(final String managerHostName, final int managerPort) {
    listenerConnected = managerHostName+":"+managerPort;
  }

  public void disconnected() {
    listenerDisconnected = true;
  }

  public void reinitialized() {
    listenerInitialized = true;
  }

  public void capturingStarted(final String sessionId) {
    listenerCapturingStarted = sessionId;
  }

  public void capturingStopped(final String sessionId) {
    listenerCapturingStopped = sessionId;
  }

  public void sessionCreated(final String sessionId) {
    listenerSessionCreated = sessionId;
  }

  public void sessionClosed(final String sessionId) {
    listenerSessionClosed = sessionId;
  }

  public void statisticsEnabled(final String[] names) {
    listenerStatisticsEnabled = names;
  }

  @Override
  protected void duringRunningCluster() throws Exception {
    waitForAllNodesToConnectToGateway(StatisticsGathererTestApp.NODE_COUNT+1);

    File tmp_dir = makeTmpDir(getClass());

    StatisticsStore store = new H2StatisticsStoreImpl(tmp_dir);
    StatisticsGatherer gatherer = new StatisticsGathererImpl(store);
    gatherer.addListener(this);

    assertNull(gatherer.getActiveSessionId());

    assertNull(listenerConnected);
    gatherer.connect("localhost", getAdminPort());
    assertEquals("localhost:"+getAdminPort(), listenerConnected);

    String[] statistics = gatherer.getSupportedStatistics();

    String sessionid1 = UUID.getUUID().toString();
    assertNull(listenerSessionCreated);
    gatherer.createSession(sessionid1);
    assertEquals(sessionid1, listenerSessionCreated);

    assertFalse(listenerInitialized);
    gatherer.reinitialize();
    assertTrue(listenerInitialized);
    assertEquals(sessionid1, listenerSessionClosed);
    assertEquals(sessionid1, listenerCapturingStopped);

    listenerSessionCreated = null;
    assertNull(listenerSessionCreated);
    gatherer.createSession(sessionid1);
    assertEquals(sessionid1, listenerSessionCreated);

    listenerSessionClosed = null;
    listenerCapturingStopped = null;

    String sessionid2 = UUID.getUUID().toString();
    assertNull(listenerCapturingStopped);
    assertNull(listenerSessionClosed);
    gatherer.createSession(sessionid2);
    assertEquals(sessionid1, listenerSessionClosed);
    assertEquals(sessionid1, listenerCapturingStopped);
    assertEquals(sessionid2, listenerSessionCreated);

    assertEquals(sessionid2, gatherer.getActiveSessionId());

    assertNull(listenerStatisticsEnabled);
    gatherer.enableStatistics(statistics);
    assertNotNull(listenerStatisticsEnabled);
    assertEquals(statistics.length, listenerStatisticsEnabled.length);
    for (int i = 0; i < statistics.length; i++) {
      assertEquals(statistics[i], listenerStatisticsEnabled[i]);
    }

    assertNull(listenerCapturingStarted);
    gatherer.startCapturing();
    assertEquals(sessionid2, listenerCapturingStarted);
    Thread.sleep(10000);
    listenerCapturingStopped = null;
    assertNull(listenerCapturingStopped);
    gatherer.stopCapturing();
    assertEquals(sessionid2, listenerCapturingStopped);

    Thread.sleep(5000);

    List<StatisticData> data_list = null;
    boolean got_all_shutdowns = false;
    do {
      final List<StatisticData> potential_data_list = new ArrayList<StatisticData>();
      store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
        public boolean useStatisticData(final StatisticData data) {
          potential_data_list.add(data);
          return true;
        }
      });

      int number_of_shutdowns = 0;
      for (StatisticData data : potential_data_list) {
        if (SRAShutdownTimestamp.ACTION_NAME.equals(data.getName())) {
          number_of_shutdowns++;
        }
      }

      got_all_shutdowns = (number_of_shutdowns == StatisticsGathererTestApp.NODE_COUNT+1);
      if (got_all_shutdowns) {
        data_list = potential_data_list;
      } else {
        Thread.sleep(3000);
      }
    } while (!got_all_shutdowns);

    listenerSessionClosed = null;
    assertNull(listenerSessionClosed);
    assertFalse(listenerDisconnected);
    gatherer.disconnect();
    assertEquals(sessionid2, listenerSessionClosed);
    assertTrue(listenerDisconnected);

    assertNull(gatherer.getActiveSessionId());

    // check the data
    System.out.println("=========================");
    System.out.println("Received Statistics Data:");
    System.out.println("-------------------------");
    for (StatisticData data : data_list) {
      System.out.println(data);
    }
    System.out.println("=========================");
    assertTrue(data_list.size() > 2);
    assertEquals(SRAStartupTimestamp.ACTION_NAME, data_list.get(0).getName());
    assertEquals(SRAShutdownTimestamp.ACTION_NAME, data_list.get(data_list.size() - 1).getName());
    Set<String> received_data_names = new HashSet<String>();
    for (int i = 1; i < data_list.size() - 1; i++) {
      StatisticData stat_data = data_list.get(i);
      received_data_names.add(stat_data.getName());
    }

    // check that there's at least one data element name per registered statistic
    // this assert is not true since there are statistics that do not have data
    // until there are some transaction between the L1 and L2.
    // e.g. SRAMessages, SRAL2FaultsFromDisk, SRADistributedGC
    //
    // commenting below assert until we simulate some messages between L1 and L2
    //assertTrue(received_data_names.size() >= statistics.length);
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