/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsManager;
import com.tc.statistics.agent.exceptions.StatisticsAgentConnectionAlreadyConnectedException;
import com.tc.statistics.agent.exceptions.StatisticsAgentConnectionConnectErrorException;
import com.tc.statistics.agent.exceptions.StatisticsAgentConnectionDisconnectErrorException;
import com.tc.statistics.agent.exceptions.StatisticsAgentConnectionException;
import com.tc.statistics.agent.exceptions.StatisticsAgentConnectionNotConnectedException;
import com.tc.statistics.agent.exceptions.StatisticsAgentConnectionToNonAgentException;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.beans.exceptions.UnknownStatisticsSessionIdException;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

public class StatisticsAgentConnection implements StatisticsManager {
  private final static TCLogger LOGGER = TCLogging.getLogger(StatisticsAgentConnection.class);

  private boolean isServerAgent = false;
  private MBeanServerConnection serverConnection = null;
  private StatisticsManagerMBean statManager = null;
  private StatisticsEmitterMBean statEmitter = null;
  private NotificationListener listener = null;

  public synchronized void enable() {
    statManager.enable();
    statEmitter.enable();
  }

  public synchronized void disable() {
    statManager.disable();
    statEmitter.disable();
  }

  public boolean isServerAgent() {
    return isServerAgent;
  }

  public synchronized void reinitialize() {
    synchronized (statEmitter) {
      boolean was_emitter_enabled = statEmitter.isEnabled();

      statEmitter.disable();

      statManager.reinitialize();

      if (was_emitter_enabled) {
        statEmitter.enable();
      }
    }
  }

  public String[] getSupportedStatistics() {
    return statManager.getSupportedStatistics();
  }

  public void createSession(final String sessionId) {
    statManager.createSession(sessionId);
  }

  private void handleMissingSessionIdException(final RuntimeMBeanException e, final String msg) {
    if (e.getCause() instanceof UnknownStatisticsSessionIdException) {
      UnknownStatisticsSessionIdException ussie = (UnknownStatisticsSessionIdException)e.getCause();
      String msg_full = msg + " for session '" + ussie.getSessionId() + "' on node '" + ussie.getNodeName() + "'";
      LOGGER.warn(msg_full);
    } else {
      throw e;
    }
  }

  public void disableAllStatistics(final String sessionId) {
    try {
      statManager.disableAllStatistics(sessionId);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to disable the statistics");
    }
  }

  public boolean enableStatistic(final String sessionId, final String name) {
    try {
      return statManager.enableStatistic(sessionId, name);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to enable the statistic '" + name + "'");
      return false;
    }
  }

  public String getStatisticType(String name) {
    try {
      return statManager.getStatisticType(name);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to retrieve the type of statistic '" + name + "'");
      return null;
    }
  }

  public StatisticData[] captureStatistic(final String sessionId, final String name) {
    try {
      return statManager.captureStatistic(sessionId, name);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to capture the statistic '" + name + "'");
      return StatisticData.EMPTY_ARRAY;
    }
  }

  public StatisticData[] retrieveStatisticData(final String name) {
    try {
      return statManager.retrieveStatisticData(name);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to retrieve the statistic '" + name + "'");
      return StatisticData.EMPTY_ARRAY;
    }
  }

  public void startCapturing(final String sessionId) {
    try {
      statManager.startCapturing(sessionId);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to start capturing");
    }
  }

  public void stopCapturing(final String sessionId) {
    try {
      statManager.stopCapturing(sessionId);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to stop capturing");
    }
  }

  public void setGlobalParam(final String key, final Object value) {
    statManager.setGlobalParam(key, value);
  }

  public Object getGlobalParam(final String key) {
    return statManager.getGlobalParam(key);
  }

  public void setSessionParam(final String sessionId, final String key, final Object value) {
    try {
      statManager.setSessionParam(sessionId, key, value);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to set the session parameter '" + key + "' to '" + value + "'");
    }
  }

  public Object getSessionParam(final String sessionId, final String key) {
    try {
      return statManager.getSessionParam(sessionId, key);
    } catch (RuntimeMBeanException e) {
      handleMissingSessionIdException(e, "Unable to get the session parameter '" + key + "'");
      return null;
    }
  }

  public void connect(final MBeanServerConnection serverConn, final NotificationListener notificationListener) throws StatisticsAgentConnectionException {
    Assert.assertNotNull("serverConnection", serverConn);
    if (statManager != null) throw new StatisticsAgentConnectionAlreadyConnectedException();

    this.serverConnection = serverConn;
    setupManagerMBean(serverConn);
    ObjectName emitter_name = setupEmitterMBean(serverConn);

    // register the statistics data listener
    this.listener = notificationListener;
    try {
      serverConn.addNotificationListener(emitter_name, notificationListener, null, null);
    } catch (Exception e) {
      throw new StatisticsAgentConnectionConnectErrorException("Unexpected error while registering the notification listener for statistics emitting.", e);
    }
  }

  private ObjectName setupManagerMBean(MBeanServerConnection mbeanServerConnection) throws StatisticsAgentConnectionException {
    // setup the statistics manager mbean
    try {
      ObjectName manager_name = findMBeanName(mbeanServerConnection, StatisticsMBeanNames.STATISTICS_MANAGER);
      if (null == manager_name) {
        throw new StatisticsAgentConnectionToNonAgentException();
      }

      statManager = (StatisticsManagerMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, manager_name, StatisticsManagerMBean.class, false);

      return manager_name;
    } catch (Exception e) {
      throw new StatisticsAgentConnectionConnectErrorException("Unexpected error while finding the manager mbean of the agent.", e);
    }
  }

  private ObjectName setupEmitterMBean(MBeanServerConnection mbeanServerConnection) throws StatisticsAgentConnectionException {
    // setup the statistics emitter mbean
    try {
      ObjectName emitter_name = findMBeanName(mbeanServerConnection, StatisticsMBeanNames.STATISTICS_EMITTER);
      if (null == emitter_name) {
        throw new StatisticsAgentConnectionToNonAgentException();
      }

      statEmitter = (StatisticsEmitterMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, emitter_name, StatisticsEmitterMBean.class, false);

      return emitter_name;
    } catch (Exception e) {
      throw new StatisticsAgentConnectionException("Unexpected error while finding the emitter mbean of the agent.", e);
    }
  }

  private ObjectName findMBeanName(MBeanServerConnection mbeanServerConnection, ObjectName baseName) throws IOException, MalformedObjectNameException {
    ObjectName manager_name = null;

    // try to find the statistics manager, by first using the standard name and if that doesn't yield any
    // result by doing a search that includes at least all the properties of the standard name
    Set manager_names = mbeanServerConnection.queryNames(baseName, null);
    if (manager_names.size() > 0) {
      manager_name = (ObjectName)manager_names.iterator().next();
    } else {
      manager_names = mbeanServerConnection.queryNames(new ObjectName(baseName.getCanonicalName() + ",*"), null);
      if (manager_names.size() > 0) {
        manager_name = (ObjectName)manager_names.iterator().next();
      }
    }
    return manager_name;
  }

  public void disconnect() throws StatisticsAgentConnectionException {
    if (null == statManager) throw new StatisticsAgentConnectionNotConnectedException();

    try {
      serverConnection.removeNotificationListener(StatisticsMBeanNames.STATISTICS_EMITTER, listener);
    } catch (Exception e) {
      throw new StatisticsAgentConnectionDisconnectErrorException("Unexpected error while removing the notification listener for statistics emitting.", e);
    } finally {
      listener = null;
      statEmitter = null;
      statManager = null;
    }
  }
}