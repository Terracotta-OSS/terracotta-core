/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans.impl;

import com.tc.config.schema.CommonL2Config;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsGathererSubSystem;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.gatherer.StatisticsGathererListener;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererException;
import com.tc.statistics.store.StatisticsStoreListener;
import com.tc.statistics.store.exceptions.StatisticsStoreException;
import com.tc.util.Assert;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public class StatisticsLocalGathererMBeanImpl extends AbstractTerracottaMBean implements StatisticsLocalGathererMBean,
    StatisticsGathererListener, StatisticsStoreListener {
  public final static MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { STATISTICS_LOCALGATHERER_STARTEDUP_TYPE,
      STATISTICS_LOCALGATHERER_SHUTDOWN_TYPE, STATISTICS_LOCALGATHERER_REINITIALIZED_TYPE,
      STATISTICS_LOCALGATHERER_CAPTURING_STARTED_TYPE, STATISTICS_LOCALGATHERER_CAPTURING_STOPPED_TYPE,
      STATISTICS_LOCALGATHERER_SESSION_CREATED_TYPE, STATISTICS_LOCALGATHERER_SESSION_CLOSED_TYPE,
      STATISTICS_LOCALGATHERER_SESSION_CLEARED_TYPE, STATISTICS_LOCALGATHERER_ALLSESSIONS_CLEARED_TYPE,
      STATISTICS_LOCALGATHERER_STORE_OPENED_TYPE, STATISTICS_LOCALGATHERER_STORE_CLOSED_TYPE };
    final String name = Notification.class.getName();
    final String description = "Each notification sent contains information about what happened with the local statistics gathering";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final AtomicLong                    sequenceNumber;

  private final StatisticsGathererSubSystem   subsystem;
  private final CommonL2Config                config;
  private final L2DSOConfig                   dsoConfig;
  private String                              username;
  private String                              password;

  public StatisticsLocalGathererMBeanImpl(final StatisticsGathererSubSystem subsystem, final CommonL2Config config,
                                          final L2DSOConfig dsoConfig) throws NotCompliantMBeanException {
    super(StatisticsLocalGathererMBean.class, true);
    Assert.assertNotNull("subsystem", subsystem);
    Assert.assertNotNull("config", config);
    sequenceNumber = new AtomicLong(0L);
    this.subsystem = subsystem;
    this.config = config;
    this.dsoConfig = dsoConfig;

    // keep at the end of the constructor to make sure that all the initialization
    // is done before registering this instance as a listener
    if (this.subsystem.isActive()) {
      this.subsystem.getStatisticsGatherer().addListener(this);
      this.subsystem.getStatisticsStore().addListener(this);
    }
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

  public void reset() {
    //
  }

  public boolean isActive() {
    return subsystem.isActive();
  }

  public void startup() {
    if (!subsystem.isActive()) { return; }

    try {
      String hostname = config.jmxPort().getBind();
      if (null == hostname) {
        hostname = dsoConfig.host();
      }
      if (username != null && password != null) {
        subsystem.getStatisticsGatherer().connect(username, password, hostname, config.jmxPort().getIntValue());
      } else {
        subsystem.getStatisticsGatherer().connect(hostname, config.jmxPort().getIntValue());
      }
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void shutdown() {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsGatherer().disconnect();
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void reinitialize() {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.reinitialize();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void createSession(final String sessionId) {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsGatherer().createSession(sessionId);
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public String getActiveSessionId() {
    if (!subsystem.isActive()) { return null; }

    return subsystem.getStatisticsGatherer().getActiveSessionId();
  }

  public String[] getAvailableSessionIds() {
    if (!subsystem.isActive()) { return new String[0]; }

    try {
      return subsystem.getStatisticsStore().getAvailableSessionIds();
    } catch (StatisticsStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public void closeSession() {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsGatherer().closeSession();
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public String[] getSupportedStatistics() {
    if (!subsystem.isActive()) { return new String[0]; }

    try {
      return subsystem.getStatisticsGatherer().getSupportedStatistics();
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void enableStatistics(final String[] names) {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsGatherer().enableStatistics(names);
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public StatisticData[] captureStatistic(final String name) {
    if (!subsystem.isActive()) { return new StatisticData[0]; }

    try {
      return subsystem.getStatisticsGatherer().captureStatistic(name);
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public StatisticData[] retrieveStatisticData(final String name) {
    if (!subsystem.isActive()) { return new StatisticData[0]; }

    try {
      return subsystem.getStatisticsGatherer().retrieveStatisticData(name);
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void startCapturing() {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsGatherer().startCapturing();
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopCapturing() {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsGatherer().stopCapturing();
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isCapturing() {
    if (!subsystem.isActive()) { return false; }

    return subsystem.getStatisticsGatherer().isCapturing();
  }

  public void setGlobalParam(final String key, final Object value) {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsGatherer().setGlobalParam(key, value);
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public Object getGlobalParam(final String key) {
    if (!subsystem.isActive()) { return null; }

    try {
      return subsystem.getStatisticsGatherer().getGlobalParam(key);
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void setSessionParam(final String key, final Object value) {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsGatherer().setSessionParam(key, value);
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public Object getSessionParam(final String key) {
    if (!subsystem.isActive()) { return null; }

    try {
      return subsystem.getStatisticsGatherer().getSessionParam(key);
    } catch (StatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void clearStatistics(final String sessionId) {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsStore().clearStatistics(sessionId);
    } catch (StatisticsStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public void clearAllStatistics() {
    if (!subsystem.isActive()) { return; }

    try {
      subsystem.getStatisticsStore().clearAllStatistics();
    } catch (StatisticsStoreException e) {
      throw new RuntimeException(e);
    }
  }

  private void createAndSendNotification(final String type, final Object data) {
    final Notification notification = new Notification(type, StatisticsLocalGathererMBeanImpl.this,
                                                       sequenceNumber.incrementAndGet(), System.currentTimeMillis());
    notification.setUserData(data);
    sendNotification(notification);
  }

  public void connected(final String managerHostName, final int managerPort) {
    createAndSendNotification(STATISTICS_LOCALGATHERER_STARTEDUP_TYPE, managerHostName + ":" + managerPort);
  }

  public void disconnected() {
    createAndSendNotification(STATISTICS_LOCALGATHERER_SHUTDOWN_TYPE, null);
  }

  public void reinitialized() {
    createAndSendNotification(STATISTICS_LOCALGATHERER_REINITIALIZED_TYPE, null);
  }

  public void capturingStarted(final String sessionId) {
    createAndSendNotification(STATISTICS_LOCALGATHERER_CAPTURING_STARTED_TYPE, sessionId);
  }

  public void capturingStopped(final String sessionId) {
    createAndSendNotification(STATISTICS_LOCALGATHERER_CAPTURING_STOPPED_TYPE, sessionId);
  }

  public void sessionCreated(final String sessionId) {
    createAndSendNotification(STATISTICS_LOCALGATHERER_SESSION_CREATED_TYPE, sessionId);
  }

  public void sessionClosed(final String sessionId) {
    createAndSendNotification(STATISTICS_LOCALGATHERER_SESSION_CLOSED_TYPE, sessionId);
  }

  public void statisticsEnabled(final String[] names) {
    createAndSendNotification(STATISTICS_LOCALGATHERER_STATISTICS_ENABLED_TYPE, names);
  }

  public void sessionCleared(final String sessionId) {
    createAndSendNotification(STATISTICS_LOCALGATHERER_SESSION_CLEARED_TYPE, sessionId);
  }

  public void allSessionsCleared() {
    createAndSendNotification(STATISTICS_LOCALGATHERER_ALLSESSIONS_CLEARED_TYPE, null);
  }

  public void opened() {
    createAndSendNotification(STATISTICS_LOCALGATHERER_STORE_OPENED_TYPE, null);
  }

  public void closed() {
    createAndSendNotification(STATISTICS_LOCALGATHERER_STORE_CLOSED_TYPE, null);
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public void setPassword(final String password) {
    this.password = password;
  }
}