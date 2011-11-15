/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatisticsRetrieverImpl implements StatisticsRetriever, StatisticsBufferListener {
  public static final String        TIMER_NAME                    = "Statistics Retriever";

  public final static int           DEFAULT_NOTIFICATION_INTERVAL = 60;

  private final static TCLogger     LOGGER                        = TCLogging.getLogger(StatisticsRetrieverImpl.class);

  private Timer                     timer;

  private final DSOStatisticsConfig config;
  private final StatisticsBuffer    buffer;
  private final String              sessionId;

  // the structure of this map is created in a synchronized fashion in the createEmptyActionsMap
  // method, outside of that method the map is only read and the values (which are lists) are being
  // modified (not replaced)
  private volatile Map              actionsMap;

  private LogRetrievalInProcessTask infoTask                      = null;
  private RetrieveStatsTask         statsTask                     = null;

  public StatisticsRetrieverImpl(final DSOStatisticsConfig config, final StatisticsBuffer buffer, final String sessionId) {
    Assert.assertNotNull("config", config);
    Assert.assertNotNull("buffer", buffer);
    Assert.assertNotNull("sessionId", sessionId);
    this.config = config;
    this.buffer = buffer;
    this.sessionId = sessionId;

    // keep at the end of the constructor to make sure that all the initialization
    // is done before registering this instance as a listener
    this.buffer.addListener(this);

    createEmptyActionsMap();
  }

  private synchronized void createEmptyActionsMap() {
    // initialize the map of actions that are organized according
    // to their type
    Map actions_map_construction = new HashMap();
    for (Iterator types_it = StatisticType.getAllTypes().iterator(); types_it.hasNext();) {
      StatisticType type = (StatisticType) types_it.next();
      actions_map_construction.put(type, new CopyOnWriteArrayList());
    }
    actionsMap = Collections.unmodifiableMap(actions_map_construction);
  }

  public String getSessionId() {
    return sessionId;
  }

  public DSOStatisticsConfig getConfig() {
    return config;
  }

  public void removeAllActions() {
    createEmptyActionsMap();
  }

  public void registerAction(final StatisticRetrievalAction action) {
    if (null == action) return;
    if (null == action.getType()) Assert.fail("Can't register an action with a null type.");

    List action_list = (List) actionsMap.get(action.getType());
    if (null == action_list) {
      Assert.fail("the actionsMap doesn't contain an entry for the statistic type '" + action.getType() + "'");
      return;
    }
    action_list.add(action);
  }

  public void startup() {
    retrieveStartupMarker();
    retrieveStartupStatistics();
    enableTimerAndTasks();
  }

  public void shutdown() {
    buffer.removeListener(this);

    disableTimerAndTasks();
  }

  public boolean containsAction(final StatisticRetrievalAction action) {
    if (null == action) return false;
    if (null == action.getType()) return false;

    List action_list = (List) actionsMap.get(action.getType());
    if (null == action_list) { return false; }
    return action_list.contains(action);
  }

  private void retrieveStartupMarker() {
    retrieveAction(new Date(), new SRAStartupTimestamp());
  }

  private void retrieveShutdownMarker(final Date moment) {
    retrieveAction(moment, new SRAShutdownTimestamp());
  }

  private void retrieveStartupStatistics() {
    List action_list = (List) actionsMap.get(StatisticType.STARTUP);
    Assert.assertNotNull("list of startup actions", action_list);
    final Date moment = new Date();
    for (Iterator actions_it = action_list.iterator(); actions_it.hasNext();) {
      retrieveAction(moment, (StatisticRetrievalAction) actions_it.next());
    }
  }

  private void retrieveAction(final Date moment, final StatisticRetrievalAction action) {
    StatisticData[] data = null;
    try {
      data = action.retrieveStatisticData();
    } catch (ThreadDeath e) {
      throw e;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable e) {
      LOGGER.error("Unexpected exception while retrieving the statistic data for SRA '" + action.getName() + "'", e);
      return;
    }
    if (data != null) {
      for (StatisticData element : data) {
        element.setSessionId(sessionId);
        element.setMoment(moment);
        bufferData(element);
      }
    }
  }

  private void bufferData(final StatisticData data) {
    try {
      buffer.storeStatistic(data);
    } catch (StatisticsBufferException e) {
      LOGGER.error("Couldn't buffer the statistic data " + data, e);
    }
  }

  private synchronized void enableTimerAndTasks() {
    if (timer != null || statsTask != null || infoTask != null) {
      disableTimerAndTasks();
    }

    timer = new Timer(TIMER_NAME, true);
    infoTask = new LogRetrievalInProcessTask();
    timer.scheduleAtFixedRate(infoTask,
                              0,
                              TCPropertiesImpl.getProperties()
                                  .getInt(TCPropertiesConsts.CVT_RETRIEVER_NOTIFICATION_INTERVAL,
                                          DEFAULT_NOTIFICATION_INTERVAL) * 1000);

    statsTask = new RetrieveStatsTask();
    timer.scheduleAtFixedRate(statsTask, 0, config.getParamLong(DSOStatisticsConfig.KEY_RETRIEVER_SCHEDULE_INTERVAL));
  }

  private synchronized void disableTimerAndTasks() {
    if (statsTask != null) {
      statsTask.shutdown();

      final long before_shutdown_wait = System.currentTimeMillis();
      final long shutdown_wait_expiration = config.getParamLong(DSOStatisticsConfig.KEY_RETRIEVER_SCHEDULE_INTERVAL) * 3;
      boolean interrupted = false;
      try {
        while (!statsTask.isShutdown()
               && (System.currentTimeMillis() - before_shutdown_wait) < shutdown_wait_expiration) { // only wait for a
                                                                                                    // limited amount of
                                                                                                    // time
          try {
            this.wait(shutdown_wait_expiration); // wait for the retriever schedule interval
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      } finally {
        if (interrupted) {
          // Restore the interrupted status of the thread for methods higher up the call stack
          Thread.currentThread().interrupt();
        }
      }

      statsTask = null;
    }

    if (infoTask != null) {
      infoTask.shutdown();
      infoTask = null;
    }

    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  public void capturingStarted(final String sessionID) {
    if (sessionID.equals(this.sessionId)) {
      startup();
    }
  }

  public void capturingStopped(final String sessionID) {
    if (sessionID.equals(this.sessionId)) {
      shutdown();
    }
  }

  public void opened() {
    //
  }

  public void closing() {
    shutdown();
  }

  public void closed() {
    //
  }

  private class RetrieveStatsTask extends TimerTask {
    private volatile boolean performTaskShutdown = false;
    private volatile boolean isShutdown          = false;

    public void shutdown() {
      performTaskShutdown = true;
    }

    public boolean isShutdown() {
      return isShutdown;
    }

    @Override
    public void run() {
      List action_list = (List) actionsMap.get(StatisticType.SNAPSHOT);
      Assert.assertNotNull("list of snapshot actions", action_list);
      final Date moment = new Date();
      for (Iterator actions_it = action_list.iterator(); actions_it.hasNext();) {
        retrieveAction(moment, (StatisticRetrievalAction) actions_it.next());
      }

      if (performTaskShutdown) {
        synchronized (StatisticsRetrieverImpl.this) {
          cancel();
          retrieveShutdownMarker(new Date(moment.getTime() + 1));
          isShutdown = true;

          StatisticsRetrieverImpl.this.notifyAll();
        }
      }
    }
  }

  private class LogRetrievalInProcessTask extends TimerTask {
    private final long       start;

    private volatile boolean shutdown = false;

    private LogRetrievalInProcessTask() {
      start = System.currentTimeMillis();
      LOGGER.info("Statistics retrieval is STARTING for session ID '" + sessionId + "' on node '"
                  + buffer.getDefaultNodeName() + "'.");
    }

    public void shutdown() {
      shutdown = true;
      LOGGER.info("Statistics retrieval has STOPPED for session ID '" + sessionId + "' on node '"
                  + buffer.getDefaultNodeName() + "' after running for "
                  + ((System.currentTimeMillis() - start) / 1000) + " seconds.");
      this.cancel();
    }

    @Override
    public void run() {
      if (!shutdown) {
        LOGGER.info("Statistics retrieval in PROCESS for session ID '" + sessionId + "' on node '"
                    + buffer.getDefaultNodeName() + "' for " + ((System.currentTimeMillis() - start) / 1000)
                    + " seconds.");
      }
    }
  }
}
