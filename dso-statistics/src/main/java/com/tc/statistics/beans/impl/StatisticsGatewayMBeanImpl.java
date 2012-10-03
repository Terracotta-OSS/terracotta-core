/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsGateway;
import com.tc.statistics.agent.StatisticsAgentConnection;
import com.tc.statistics.agent.exceptions.StatisticsAgentConnectionException;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.TopologyChangeHandler;
import com.tc.util.concurrent.CopyOnWriteSequentialMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;

public class StatisticsGatewayMBeanImpl extends AbstractTerracottaMBean implements StatisticsGatewayMBean,
    StatisticsGateway, NotificationListener {

  private final static TCLogger          LOGGER                = TCLogging.getLogger(StatisticsGatewayMBeanImpl.class);

  private final AtomicLong               sequenceNumber        = new AtomicLong(0L);

  private volatile CopyOnWriteSequentialMap<ChannelID, StatisticsAgentConnection> agents                = new CopyOnWriteSequentialMap<ChannelID, StatisticsAgentConnection>();
  private volatile TopologyChangeHandler topologyChangeHandler = null;

  public StatisticsGatewayMBeanImpl() throws NotCompliantMBeanException {
    super(StatisticsGatewayMBean.class, true, false);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return StatisticsEmitterMBeanImpl.NOTIFICATION_INFO;
  }

  @Override
  public void reinitialize() {
    for (StatisticsAgentConnection element : agents.values()) {
      element.reinitialize();
    }
  }

  @Override
  public void setTopologyChangeHandler(final TopologyChangeHandler handler) {
    this.topologyChangeHandler = handler;
  }

  @Override
  public void clearTopologyChangeHandler() {
    topologyChangeHandler = null;
  }

  @Override
  public void addStatisticsAgent(final ChannelID channelId, final MBeanServerConnection mbeanServerConnection) {
    StatisticsAgentConnection agent = new StatisticsAgentConnection();
    try {
      agent.connect(mbeanServerConnection, this);
    } catch (StatisticsAgentConnectionException e) {
      LOGGER.warn("Unable to add statistics agent for channel ID '" + channelId + "' to the gateway.", e);
      return;
    }

    agents.put(channelId, agent);

    if (topologyChangeHandler != null) {
      try {
        topologyChangeHandler.agentAdded(agent);
      } catch (Exception e) {
        LOGGER.warn("Unexpected error while configuring the statistics agent for channel ID '" + channelId
                    + "' after it was added to the gateway.", e);
      }
    }
  }

  @Override
  public long[] getConnectedAgentChannelIDs() {
    final Set<ChannelID> channelIDs = agents.keySet();
    final long[] result = new long[channelIDs.size()];
    int i = 0;
    for (ChannelID chId : channelIDs) {
      result[i++] = chId.toLong();
    }
    return result;
  }

  @Override
  public void removeStatisticsAgent(final ChannelID channelId) {
    agents.remove(channelId);
  }

  @Override
  public void cleanup() {

    for (StatisticsAgentConnection sac : agents.values()) {
      try {
        sac.disconnect();
      } catch (StatisticsAgentConnectionException e) {
        LOGGER.warn("Unable to disconnect statistics agent from the gateway.", e);
      }
    }
    agents.clear();
  }

  @Override
  protected void enabledStateChanged() {
    for (StatisticsAgentConnection agent : agents.values()) {
      if (isEnabled()) {
        agent.enable();
      } else {
        agent.disable();
      }
    }
  }

  @Override
  public void reset() {
    //
  }

  @Override
  public String[] getSupportedStatistics() {
    Set combinedStats = new TreeSet();

    for (StatisticsAgentConnection agent : agents.values()) {
      String[] agentStats = agent.getSupportedStatistics();
      for (String agentStat : agentStats) {
        combinedStats.add(agentStat);
      }
    }

    return (String[]) combinedStats.toArray(new String[combinedStats.size()]);
  }

  @Override
  public void createSession(final String sessionId) {
    for (StatisticsAgentConnection agent : agents.values()) {
      agent.createSession(sessionId);
    }
  }

  @Override
  public void disableAllStatistics(final String sessionId) {
    for (StatisticsAgentConnection agent : agents.values()) {
      agent.disableAllStatistics(sessionId);
    }
  }

  @Override
  public boolean enableStatistic(final String sessionId, final String name) {
    boolean result = false;
    for (StatisticsAgentConnection agent : agents.values()) {
      if (agent.enableStatistic(sessionId, name)) {
        result = true;
      }
    }
    return result;
  }

  @Override
  public String getStatisticType(final String name) {
    for (StatisticsAgentConnection agent : agents.values()) {
      String type = agent.getStatisticType(name);
      if (type != null) { return type; }
    }

    return null;
  }

  @Override
  public StatisticData[] captureStatistic(final String sessionId, final String name) {
    List result_list = new ArrayList();

    for (StatisticsAgentConnection agent : agents.values()) {
      StatisticData[] data = agent.captureStatistic(sessionId, name);
      if (data != null) {
        for (StatisticData element : data) {
          result_list.add(element);
        }
      }
    }

    return (StatisticData[]) result_list.toArray(new StatisticData[result_list.size()]);
  }

  @Override
  public StatisticData[] retrieveStatisticData(final String name) {
    List result_list = new ArrayList();

    for (StatisticsAgentConnection agent : agents.values()) {
      StatisticData[] data = agent.retrieveStatisticData(name);
      if (data != null) {
        for (StatisticData element : data) {
          result_list.add(element);
        }
      }
    }

    return (StatisticData[]) result_list.toArray(new StatisticData[result_list.size()]);
  }

  @Override
  public void startCapturing(final String sessionId) {
    for (StatisticsAgentConnection agent : agents.values()) {
      agent.startCapturing(sessionId);
    }
  }

  @Override
  public void stopCapturing(final String sessionId) {
    for (StatisticsAgentConnection agent : agents.values()) {
      agent.stopCapturing(sessionId);
    }
  }

  @Override
  public void setGlobalParam(final String key, final Object value) {
    for (StatisticsAgentConnection agent : agents.values()) {
      agent.setGlobalParam(key, value);
    }
  }

  @Override
  public Object getGlobalParam(final String key) {
    if (0 == agents.size()) { return null; }

    for (StatisticsAgentConnection agent : agents.values()) {
      if (agent.isServerAgent()) { return agent.getGlobalParam(key); }
    }

    LOGGER
        .warn("Unable to find the L2 server agent, this means that there's no authoritative agent to retrieve the global parameter '"
              + key + "' from.");
    StatisticsAgentConnection agent = agents.values().iterator().next();
    return agent.getGlobalParam(key);
  }

  @Override
  public void setSessionParam(final String sessionId, final String key, final Object value) {
    for (StatisticsAgentConnection agent : agents.values()) {
      agent.setSessionParam(sessionId, key, value);
    }
  }

  @Override
  public Object getSessionParam(final String sessionId, final String key) {
    if (0 == agents.size()) { return null; }

    for (StatisticsAgentConnection agent : agents.values()) {
      if (agent.isServerAgent()) { return agent.getSessionParam(sessionId, key); }
    }

    LOGGER
        .warn("Unable to find the L2 server agent, this means that there's no authoritative agent to retrieve the parameter '"
              + key + "' from for session '" + sessionId + "'.");
    StatisticsAgentConnection agent = agents.values().iterator().next();
    return agent.getSessionParam(sessionId, key);
  }

  @Override
  public void handleNotification(final Notification notification, final Object o) {
    notification.setSequenceNumber(sequenceNumber.incrementAndGet());
    sendNotification(notification);
  }
}