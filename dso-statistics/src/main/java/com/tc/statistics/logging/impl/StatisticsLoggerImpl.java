/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.logging.impl;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.logging.StatisticsLogger;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class StatisticsLoggerImpl implements StatisticsLogger {
  private final static TCLogger  LOGGER     = TCLogging.getLogger(StatisticsLoggerImpl.class);
  private final static TCLogger  DSO_LOGGER = CustomerLogging.getDSOGenericLogger();

  private final Timer            timer      = new Timer("Statistics Logger", true);
  private final DSOStatisticsConfig config;
  private final Set              actions    = Collections.synchronizedSet(new LinkedHashSet());

  private LogActionDataTask      logTask    = null;

  public StatisticsLoggerImpl(final DSOStatisticsConfig config) {
    Assert.assertNotNull("config", config);
    this.config = config;
  }

  public DSOStatisticsConfig getConfig() {
    return config;
  }

  public void removeAllActions() {
    actions.clear();
  }

  public void registerAction(final StatisticRetrievalAction action) {
    if (null == action) return;
    actions.add(action);
  }

  public void startup() {
    enableTimerTasks();
  }

  public void shutdown() {
    disableTimerTasks();
  }

  private synchronized void enableTimerTasks() {
    if (logTask != null) {
      disableTimerTasks();
    }

    final int period_seconds = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.CVT_STATISTICS_LOGGING_INTERVAL, DEFAULT_LOGGING_INTERVAL);
    if (period_seconds <= 0) {
      DSO_LOGGER.info("Statistics logger disabled due to property '"
                      + TCPropertiesConsts.CVT_STATISTICS_LOGGING_INTERVAL + "' not being greater than zero, it was '"
                      + period_seconds + "'");
      return;
    }
    logTask = new LogActionDataTask();
    timer.scheduleAtFixedRate(logTask, 0, period_seconds * 1000);
  }

  private synchronized void disableTimerTasks() {
    if (logTask != null) {
      logTask.shutdown();
      logTask = null;
    }

    timer.cancel();
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
        element.setMoment(moment);
        logData(element);
      }
    }
  }

  private void logData(final StatisticData data) {
    String log_data = data.toLog();
    DSO_LOGGER.info(log_data);
  }

  public boolean containsAction(final StatisticRetrievalAction action) {
    if (null == action) return false;
    return actions.contains(action);
  }

  private class LogActionDataTask extends TimerTask {
    private volatile boolean shutdown = false;

    public void shutdown() {
      shutdown = true;
      this.cancel();
    }

    @Override
    public void run() {
      if (!shutdown) {
        synchronized (actions) {
          for (final Iterator it = actions.iterator(); it.hasNext();) {
            StatisticRetrievalAction action = (StatisticRetrievalAction) it.next();
            retrieveAction(new Date(), action);
          }
        }
      }
    }
  }
}
