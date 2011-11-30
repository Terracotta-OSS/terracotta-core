/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.config.schema.StatisticsConfig;
import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.StatisticsManager;
import com.tc.statistics.StatisticsSystemType;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.statistics.retrieval.actions.SRAThreadDump;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.UUID;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;

public class AgentStatisticsManagerTest extends TCTestCase {

  private AgentStatisticsManager   agentManager;
  private StatisticsManager        statisticsManager;
  private MBeanServer              beanServer;
  private StatisticsAgentSubSystem agentSubSystem;

  @Override
  protected void setUp() throws Exception {
    agentSubSystem = new StatisticsAgentSubSystemImpl();
    agentSubSystem.setup(StatisticsSystemType.CLIENT, new StatisticsConfig() {
      public File statisticsPath() {
        try {
          return AgentStatisticsManagerTest.this.getTempDirectory();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        // return new FileConfigItem() {
        // public File getFile() {
        // try {
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        // }
        //
        // public Object getObject() {
        // return null;
        // }
        //
        // public void addListener(ConfigItemListener changeListener) {
        // //
        // }
        //
        // public void removeListener(ConfigItemListener changeListener) {
        // //
        // }
        // };
      }
    });
    populateStatisticsRegistry(agentSubSystem.getStatisticsRetrievalRegistry());

    beanServer = ManagementFactory.getPlatformMBeanServer();
    agentSubSystem.registerMBeans(beanServer);

    statisticsManager = MBeanServerInvocationHandler.newProxyInstance(beanServer,
                                                                      StatisticsMBeanNames.STATISTICS_MANAGER,
                                                                      StatisticsManager.class, false);
    agentManager = agentSubSystem.getStatisticsManager();

    agentSubSystem.setDefaultAgentDifferentiator("agentDiff");
    agentSubSystem.setDefaultAgentIp("10.0.0.1");
  }

  private void populateStatisticsRegistry(StatisticsRetrievalRegistry statisticsRetrievalRegistry) {
    statisticsRetrievalRegistry.registerActionInstance(new SRACacheObjectsEvicted());
    statisticsRetrievalRegistry.registerActionInstance(new SRACacheObjectsEvictRequest());
    statisticsRetrievalRegistry.registerActionInstance(new SRAThreadDump());
  }

  public void testGetActiveSessions() {

    String sessionId1 = UUID.getUUID().toString();
    String sessionId2 = UUID.getUUID().toString();
    String sessionId3 = UUID.getUUID().toString();
    String[] sessions = new String[] { sessionId1, sessionId2, sessionId3 };

    final String[] supportedStats = statisticsManager.getSupportedStatistics();

    for (String session : sessions) {
      statisticsManager.createSession(session);
      for (String supportedStat : supportedStats) {
        // System.out.println("Enabling " + supportedStats[j] + " for " + sessions[i]);
        statisticsManager.enableStatistic(session, supportedStat);
      }
    }
    System.out.println("Number of active sessions: " + sessions.length);
    System.out.println("Number of supported statistics: " + supportedStats.length);
    for (String supportedStat : supportedStats) {
      Collection activeSessions = agentManager.getActiveSessionIDsForAction(supportedStat);
      Assert.assertEquals("Number of active sessions should be same as activated", sessions.length,
                          activeSessions.size());
      Assert.eval("Active sessions should be same as activated.", activeSessions.containsAll(Arrays.asList(sessions)));
    }
  }

  public void testInjectStatistics() throws Exception {

    String sessionId1 = UUID.getUUID().toString();
    String sessionId2 = UUID.getUUID().toString();
    String sessionId3 = UUID.getUUID().toString();
    String[] sessions = new String[] { sessionId1, sessionId2, sessionId3 };

    StatisticsEmitterMBean statEmitter = MBeanServerInvocationHandler
        .newProxyInstance(beanServer, StatisticsMBeanNames.STATISTICS_EMITTER, StatisticsEmitterMBean.class, false);

    List<StatisticData> data = new ArrayList<StatisticData>();
    CollectingNotificationListener listener = new CollectingNotificationListener(sessions.length);
    beanServer.addNotificationListener(StatisticsMBeanNames.STATISTICS_EMITTER, listener, null, data);
    statEmitter.enable();

    final String[] supportedStats = statisticsManager.getSupportedStatistics();

    for (String session : sessions) {
      statisticsManager.createSession(session);
      for (String supportedStat : supportedStats) {
        // System.out.println("Enabling " + supportedStats[j] + " for " + sessions[i]);
        statisticsManager.enableStatistic(session, supportedStat);
      }
    }
    System.out.println("Number of active sessions: " + sessions.length);
    System.out.println("Number of supported statistics: " + supportedStats.length);

    for (String session : sessions) {
      statisticsManager.startCapturing(session);
      System.out.println("Started Capture for " + session);
      for (String supportedStat : supportedStats) {
        agentManager.injectStatisticData(session, getStatisticData(supportedStat));
      }
    }

    // wait for 10 secs
    Thread.sleep(10000);

    // stop capturing and wait for the last data
    synchronized (listener) {
      for (String session : sessions) {
        statisticsManager.stopCapturing(session);
      }
      while (!listener.getShutdown()) {
        listener.wait(2000);
      }
    }

    // disable the notification and detach the listener
    statEmitter.disable();
    beanServer.removeNotificationListener(StatisticsMBeanNames.STATISTICS_EMITTER, listener);

    System.out.println("Sessions created: " + Arrays.asList(sessions));
    // for (StatisticData sd : data) {
    // System.out.println(sd.toString());
    // }
    int numDatas = sessions.length * (2 + supportedStats.length); // 1 startup action, 1 shutdown action and
                                                                  // supportedStats.length injected actions for each
                                                                  // session
    Assert.assertEquals(numDatas, data.size());

    Map<String, Set<String>> receivedData = new HashMap<String, Set<String>>();
    for (int i = 0; i < data.size(); i++) {
      StatisticData statisticData = data.get(i);
      if (!SRAStartupTimestamp.ACTION_NAME.equals(statisticData.getName())
          && !SRAShutdownTimestamp.ACTION_NAME.equals(statisticData.getName())) {
        Set<String> actions = receivedData.get(statisticData.getSessionId());
        if (actions == null) {
          actions = new HashSet<String>();
        }
        actions.add(statisticData.getName());
        receivedData.put(statisticData.getSessionId(), actions);
      }
    }
    // check that there's at least one data element name per supported statistic for each session
    assertTrue("Number of unique sessions should be same as number of sessions created",
               receivedData.keySet().size() == sessions.length);
    for (Set<String> actions : receivedData.values()) {
      // System.out.println("Actions: "+ actions.toString());
      Assert.assertEquals("Only enabled statistics should be collected", supportedStats.length, actions.size());
    }
  }

  private StatisticData getStatisticData(String actionName) {
    StatisticData data = new StatisticData();
    data.name(actionName).moment(new Date()).data("junk");
    return data;
  }

  @Override
  protected void tearDown() throws Exception {
    agentManager = null;
    statisticsManager = null;
    agentSubSystem.cleanup();
    agentSubSystem.unregisterMBeans(beanServer);
    beanServer = null;
    agentSubSystem = null;
  }
}
