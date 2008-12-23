/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.beans.impl;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

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
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;

public class StatisticsGatewayMBeanImpl extends AbstractTerracottaMBean implements StatisticsGatewayMBean, StatisticsGateway, NotificationListener {

  private final static TCLogger LOGGER = TCLogging.getLogger(StatisticsGatewayMBeanImpl.class);

  private final SynchronizedLong sequenceNumber = new SynchronizedLong(0L);

  private volatile Map agents = new CopyOnWriteArrayMap();
  private volatile TopologyChangeHandler topologyChangeHandler = null;

  public StatisticsGatewayMBeanImpl() throws NotCompliantMBeanException {
    super(StatisticsGatewayMBean.class, true, false);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return StatisticsEmitterMBeanImpl.NOTIFICATION_INFO;
  }

  public void reinitialize() {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      ((StatisticsAgentConnection)it.next()).reinitialize();
    }
  }

  public void setTopologyChangeHandler(final TopologyChangeHandler handler) {
    this.topologyChangeHandler = handler;
  }

  public void clearTopologyChangeHandler() {
    topologyChangeHandler = null;
  }

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
        LOGGER.warn("Unexpected error while configuring the statistics agent for channel ID '" + channelId + "' after it was added to the gateway.", e);
      }
    }
  }

  public long[] getConnectedAgentChannelIDs() {
    final Set channelIDs = agents.keySet();
    final long[] result = new long[channelIDs.size()];
    int i = 0;
    for (final Iterator it = channelIDs.iterator(); it.hasNext(); ) {
      result[i++] = ((ChannelID)it.next()).toLong();
    }
    return result;
  }

  public void removeStatisticsAgent(final ChannelID channelId) {
    agents.remove(channelId);
  }

  public void cleanup() {
    Map old_agents = agents;
    agents = new CopyOnWriteArrayMap();

    for (Iterator it = old_agents.values().iterator(); it.hasNext(); ) {
      try {
        ((StatisticsAgentConnection)it.next()).disconnect();
      } catch (StatisticsAgentConnectionException e) {
        LOGGER.warn("Unable to disconnect statistics agent from the gateway.", e);
      }
    }
  }

  @Override
  protected void enabledStateChanged() {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      if (isEnabled()) {
        agent.enable();
      } else {
        agent.disable();
      }
    }
  }

  public void reset() {
    //
  }

  public String[] getSupportedStatistics() {
    Set combinedStats = new TreeSet();

    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      String[] agentStats = agent.getSupportedStatistics();
      for (String agentStat : agentStats) {
        combinedStats.add(agentStat);
      }
    }

    return (String[])combinedStats.toArray(new String[combinedStats.size()]);
  }

  public void createSession(final String sessionId) {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.createSession(sessionId);
    }
  }

  public void disableAllStatistics(final String sessionId) {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.disableAllStatistics(sessionId);
    }
  }

  public boolean enableStatistic(final String sessionId, final String name) {
    boolean result = false;
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      if (agent.enableStatistic(sessionId, name)) {
        result = true;
      }
    }
    return result;
  }

  public String getStatisticType(final String name) {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      String type = agent.getStatisticType(name);
      if (type != null) {
        return type;
      }
    }

    return null;
  }

  public StatisticData[] captureStatistic(final String sessionId, final String name) {
    List result_list = new ArrayList();

    for (Iterator agent_it = agents.values().iterator(); agent_it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)agent_it.next();
      StatisticData[] data = agent.captureStatistic(sessionId, name);
      if (data != null) {
        for (StatisticData element : data) {
          result_list.add(element);
        }
      }
    }

    return (StatisticData[])result_list.toArray(new StatisticData[result_list.size()]);
  }

  public StatisticData[] retrieveStatisticData(final String name) {
    List result_list = new ArrayList();

    for (Iterator agent_it = agents.values().iterator(); agent_it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)agent_it.next();
      StatisticData[] data = agent.retrieveStatisticData(name);
      if (data != null) {
        for (StatisticData element : data) {
          result_list.add(element);
        }
      }
    }

    return (StatisticData[])result_list.toArray(new StatisticData[result_list.size()]);
  }

  public void startCapturing(final String sessionId) {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.startCapturing(sessionId);
    }
  }

  public void stopCapturing(final String sessionId) {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.stopCapturing(sessionId);
    }
  }

  public void setGlobalParam(final String key, final Object value) {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.setGlobalParam(key, value);
    }
  }

  public Object getGlobalParam(final String key) {
    if (0 == agents.size()) {
      return null;
    }

    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      if (agent.isServerAgent()) {
        return agent.getGlobalParam(key);
      }
    }

    LOGGER.warn("Unable to find the L2 server agent, this means that there's no authoritative agent to retrieve the global parameter '" + key + "' from.");
    StatisticsAgentConnection agent = (StatisticsAgentConnection)agents.values().iterator().next();
    return agent.getGlobalParam(key);
  }

  public void setSessionParam(final String sessionId, final String key, final Object value) {
    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.setSessionParam(sessionId, key, value);
    }
  }

  public Object getSessionParam(final String sessionId, final String key) {
    if (0 == agents.size()) {
      return null;
    }

    for (Iterator it = agents.values().iterator(); it.hasNext(); ) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      if (agent.isServerAgent()) {
        return agent.getSessionParam(sessionId, key);
      }
    }

    LOGGER.warn("Unable to find the L2 server agent, this means that there's no authoritative agent to retrieve the parameter '" + key + "' from for session '" + sessionId + "'.");
    StatisticsAgentConnection agent = (StatisticsAgentConnection)agents.values().iterator().next();
    return agent.getSessionParam(sessionId, key);
  }

  public void handleNotification(final Notification notification, final Object o) {
    notification.setSequenceNumber(sequenceNumber.increment());
    sendNotification(notification);
  }
}